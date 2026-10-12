package com.heban.app.ui.discover.ledger;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.heban.app.R;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.databinding.FragmentAddEntryBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * D11 记一笔（原型 AddEntry.dc.html）。参数：{@link NavArgs#ENTRY_ID}（编辑）、{@link NavArgs#ENTRY_TYPE}（预选收支）、
 * {@link NavArgs#CROP_ID}（预选作物，可选）。保存后返回上一页并提示「已记一笔」；游客打开或保存时引导登录。
 */
public class AddEntryFragment extends BaseFragment {
    private static final String TAG_DATE = "add_entry_date";

    private FragmentAddEntryBinding binding;
    private AddEntryViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEntryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(AddEntryViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        Bundle args = getArguments();
        long entryId = args == null ? 0 : args.getLong(NavArgs.ENTRY_ID, 0);
        String entryType = args == null ? null : args.getString(NavArgs.ENTRY_TYPE);
        long cropId = args == null ? 0 : args.getLong(NavArgs.CROP_ID, 0);
        vm.start(entryId, entryType, cropId);

        setUpToolbar();
        setUpType();
        setUpCategories();
        setUpChips();
        setUpKeypad();

        vm.toast.observe(getViewLifecycleOwner(), this::showMessage);
        vm.messageRes.observe(getViewLifecycleOwner(), res -> showMessage(getString(res)));
        vm.saved.observe(getViewLifecycleOwner(), update -> finishWithMessage(
                getString(Boolean.TRUE.equals(update) ? R.string.add_entry_updated : R.string.add_entry_saved)));
        vm.deleted.observe(getViewLifecycleOwner(), d -> finishWithMessage(getString(R.string.add_entry_deleted)));
        vm.finish.observe(getViewLifecycleOwner(), res -> finishWithMessage(getString(res)));

        // 游客只能浏览：打开即提示登录
        if (savedInstanceState == null && !container().currentUser.isSignedIn()) LoginRequired.check(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showMessage(@Nullable String text) {
        if (binding != null && text != null) Snackbars.show(binding.getRoot(), text);
    }

    // ---------- 顶栏 ----------

    private void setUpToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        if (vm.isEditing()) {
            binding.toolbar.inflateMenu(R.menu.add_entry_toolbar);
            binding.toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() != R.id.action_delete) return false;
                confirmDelete();
                return true;
            });
        }
    }

    private void confirmDelete() {
        if (!LoginRequired.check(this)) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ledger_delete_title)
                .setMessage(getString(R.string.ledger_delete_message,
                        LedgerFormat.entryHeader(vm.type.getValue(), vm.category.getValue()), vm.amountText.getValue()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> vm.delete())
                .show();
    }

    // ---------- 支出 / 收入 ----------

    private void setUpType() {
        binding.groupType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            vm.selectType(checkedId == R.id.btn_in ? LedgerEntryEntity.TYPE_IN : LedgerEntryEntity.TYPE_OUT);
        });
        vm.type.observe(getViewLifecycleOwner(), t -> {
            boolean income = LedgerFormat.isIncome(t);
            int id = income ? R.id.btn_in : R.id.btn_out;
            if (binding.groupType.getCheckedButtonId() != id) binding.groupType.check(id);
            // 收入金额用 Primary，支出用正文色（UI 设计文档表 7-1）
            binding.tvAmount.setTextColor(MaterialColors.getColor(binding.tvAmount, income
                    ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurface));
            renderCategories();
        });
    }

    // ---------- 类别 ----------

    private void setUpCategories() {
        vm.category.observe(getViewLifecycleOwner(), c -> renderCategories());
    }

    /** 按当前收支类型重建类别格子（支出 8 类、收入 4 类） */
    private void renderCategories() {
        if (binding == null) return;
        String type = vm.type.getValue();
        String[] names = LedgerCategoryIcons.categories(type);
        String selected = vm.category.getValue();
        ViewGroup grid = binding.gridCategories;
        if (grid.getChildCount() != names.length) {
            grid.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(grid.getContext());
            for (int i = 0; i < names.length; i++) inflater.inflate(R.layout.add_entry_item_category, grid, true);
        }
        int primary = MaterialColors.getColor(grid, androidx.appcompat.R.attr.colorPrimary);
        int onPrimary = MaterialColors.getColor(grid, com.google.android.material.R.attr.colorOnPrimary);
        int container = MaterialColors.getColor(grid, com.google.android.material.R.attr.colorSurfaceContainerHighest);
        int onVariant = MaterialColors.getColor(grid, com.google.android.material.R.attr.colorOnSurfaceVariant);
        int onSurface = MaterialColors.getColor(grid, com.google.android.material.R.attr.colorOnSurface);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            boolean on = name.equals(selected);
            View cell = grid.getChildAt(i);
            cell.findViewById(R.id.circle).setBackgroundTintList(ColorStateList.valueOf(on ? primary : container));
            ImageView icon = cell.findViewById(R.id.icon);
            icon.setImageResource(LedgerCategoryIcons.icon(type, name));
            icon.setImageTintList(ColorStateList.valueOf(on ? onPrimary : onVariant));
            TextView label = cell.findViewById(R.id.name);
            label.setText(name);
            label.setTextColor(on ? onSurface : onVariant);
            cell.setContentDescription(name);
            cell.setSelected(on);
            cell.setOnClickListener(v -> vm.selectCategory(name));
        }
    }

    // ---------- 作物 · 日期 · 备注 ----------

    private void setUpChips() {
        View.OnClickListener pickCrop = v -> showCropPicker();
        binding.chipCrop.setOnClickListener(pickCrop);
        binding.chipCrop.setOnCloseIconClickListener(pickCrop);
        vm.crop.observe(getViewLifecycleOwner(), c -> binding.chipCrop.setText(c == null
                ? getString(R.string.add_entry_crop_none) : LedgerFormat.cropLabel(c.name, c.plotName)));

        binding.chipDate.setOnClickListener(v -> showDatePicker());
        vm.date.observe(getViewLifecycleOwner(), d -> {
            if (d != null) binding.chipDate.setText(LedgerFormat.dateChip(d, vm.today()));
        });
        reattachDatePicker();

        binding.chipNote.setOnClickListener(v -> showNoteDialog());
        vm.note.observe(getViewLifecycleOwner(), n -> {
            boolean empty = n == null || n.isEmpty();
            binding.chipNote.setText(empty ? getString(R.string.add_entry_note_add) : n);
            binding.chipNote.setTextColor(MaterialColors.getColor(binding.chipNote, empty
                    ? com.google.android.material.R.attr.colorOnSurfaceVariant
                    : com.google.android.material.R.attr.colorOnSurface));
        });
    }

    private void showCropPicker() {
        List<CropCard> list = vm.crops.getValue();
        if (list == null) return;
        String[] labels = new String[list.size() + 1];
        labels[0] = getString(R.string.add_entry_crop_none);
        CropCard current = vm.crop.getValue();
        int checked = 0;
        for (int i = 0; i < list.size(); i++) {
            CropCard c = list.get(i);
            labels[i + 1] = LedgerFormat.cropLabel(c.name, c.plotName);
            if (current != null && current.id == c.id) checked = i + 1;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_entry_crop_pick)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    vm.selectCrop(which == 0 ? null : list.get(which - 1));
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDatePicker() {
        if (getChildFragmentManager().findFragmentByTag(TAG_DATE) != null) return;
        LocalDate current = vm.date.getValue() == null ? vm.today() : vm.date.getValue();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.add_entry_date_pick)
                .setSelection(current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                        .setValidator(DateValidatorPointBackward.now())
                        .build())
                .build();
        attachDateListener(picker);
        picker.show(getChildFragmentManager(), TAG_DATE);
    }

    private void attachDateListener(MaterialDatePicker<Long> picker) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel != null) vm.setDate(Instant.ofEpochMilli(sel).atZone(ZoneOffset.UTC).toLocalDate());
        });
    }

    /** 旋转屏幕后日期选择器由系统恢复，重新挂上回调 */
    @SuppressWarnings("unchecked")
    private void reattachDatePicker() {
        Fragment f = getChildFragmentManager().findFragmentByTag(TAG_DATE);
        if (f instanceof MaterialDatePicker) attachDateListener((MaterialDatePicker<Long>) f);
    }

    private void showNoteDialog() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.add_entry_dialog_note, null, false);
        EditText input = content.findViewById(R.id.et_note);
        String note = vm.note.getValue();
        input.setText(note == null ? "" : note);
        input.setSelection(input.getText().length());
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_entry_note_add)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> vm.setNote(input.getText().toString()))
                .create();
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            vm.setNote(input.getText().toString());
            dialog.dismiss();
            return true;
        });
        dialog.setOnShowListener(d -> {
            input.requestFocus();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(
                        android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        });
        dialog.show();
    }

    // ---------- 数字键盘 ----------

    private void setUpKeypad() {
        MaterialButton[] digits = {binding.key0, binding.key1, binding.key2, binding.key3, binding.key4,
                binding.key5, binding.key6, binding.key7, binding.key8, binding.key9};
        for (int i = 0; i < digits.length; i++) {
            String key = String.valueOf(i);
            digits[i].setOnClickListener(v -> vm.press(key));
        }
        binding.key00.setOnClickListener(v -> vm.press(AmountInput.KEY_DOUBLE_ZERO));
        binding.keyDot.setOnClickListener(v -> vm.press(AmountInput.KEY_DOT));
        binding.keyBack.setOnClickListener(v -> vm.backspace());
        binding.keyBack.setOnLongClickListener(v -> {
            vm.clear();
            return true;
        });
        binding.keyClear.setOnClickListener(v -> vm.clear());
        binding.keySave.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            vm.save();
        });
    }

    // ---------- 返回 ----------

    /** 返回上一页并提示：上一页是 D10 时由 D10 显示（挂在「记一笔」上方），否则显示在 Activity 底部 */
    private void finishWithMessage(@NonNull String text) {
        NavController nav = nav();
        NavBackStackEntry previous = nav.getPreviousBackStackEntry();
        if (previous != null && previous.getDestination().getId() == R.id.ledgerFragment) {
            previous.getSavedStateHandle().set(LedgerFragment.RESULT_MESSAGE, text);
            nav.navigateUp();
            return;
        }
        View content = requireActivity().findViewById(android.R.id.content);
        nav.navigateUp();
        content.post(() -> Snackbar.make(content, text, Snackbar.LENGTH_LONG).show());
    }
}
