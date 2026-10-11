package com.heban.app.ui.crop;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.heban.app.R;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.FarmType;
import com.heban.app.databinding.FragmentAddRecordBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.BindingAdapters;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * C04 添加农事记录（原型 AddRecord.dc.html）。参数：{@link NavArgs#CROP_ID}（预选作物，从作物详情进入时不可更换）、
 * {@link NavArgs#FARM_TYPE}（预选记录类型）。保存后返回上一页并显示 Snackbar。
 */
public class AddRecordFragment extends BaseFragment {
    private static final String TAG_DATE = "add_record_date";
    private static final String TAG_TIME = "add_record_time";
    private static final FarmType[] TYPES = FarmType.values();

    private FragmentAddRecordBinding binding;
    private AddRecordViewModel vm;
    private MaterialButton[] typeButtons;
    private AddCropSuggestionAdapter materialAdapter;
    @StringRes private int materialLabel;
    @StringRes private int amountLabel;

    /** 系统照片选择器，一次最多 9 张（超出剩余名额的部分由 ViewModel 丢弃并提示） */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotos = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(AddRecordViewModel.MAX_PHOTOS),
            uris -> viewModel().addPhotos(uris));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddRecordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel();
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        Bundle args = getArguments();
        long cropId = args == null ? 0 : args.getLong(NavArgs.CROP_ID, 0);
        String farmType = args == null ? null : args.getString(NavArgs.FARM_TYPE);
        vm.start(cropId, farmType);

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.btnSave.setOnClickListener(v -> {
            if (!LoginRequired.check(this)) return;
            AddCropFormUi.hideKeyboard(binding.getRoot());
            vm.save();
        });

        setUpCrop();
        setUpTypes();
        setUpDate();
        setUpFields();
        setUpSync();
        setUpPhotos();
        observeErrors();

        vm.messageRes.observe(getViewLifecycleOwner(), res -> showMessage(getString(res)));
        vm.toast.observe(getViewLifecycleOwner(), this::showMessage);
        vm.photoLimit.observe(getViewLifecycleOwner(),
                max -> showMessage(getString(R.string.add_record_photos_limit, max)));
        vm.saved.observe(getViewLifecycleOwner(), synced -> AddCropFormUi.finishWithMessage(requireActivity(), nav(),
                getString(Boolean.TRUE.equals(synced) ? R.string.add_record_saved_synced : R.string.add_record_saved)));

        // 游客只能浏览：打开即提示登录
        if (savedInstanceState == null && !container().currentUser.isSignedIn()) LoginRequired.check(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        typeButtons = null;
        materialAdapter = null;
        binding = null;
    }

    private AddRecordViewModel viewModel() {
        if (vm == null) vm = viewModel(AddRecordViewModel.class);
        return vm;
    }

    private void showMessage(@Nullable String text) {
        if (binding != null && text != null) Snackbars.show(binding.getRoot(), text);
    }

    // ---------- 作物 ----------

    private void setUpCrop() {
        vm.crop.observe(getViewLifecycleOwner(), c -> renderCrop());
        vm.crops.observe(getViewLifecycleOwner(), list -> renderCrop());
        binding.cardCrop.setOnClickListener(v -> onCropClicked());
    }

    private void renderCrop() {
        CropCard c = vm.crop.getValue();
        List<CropCard> list = vm.crops.getValue();
        boolean loaded = list != null;
        boolean empty = loaded && list.isEmpty();
        boolean choosable = c == null || !vm.isCropPreset();
        binding.cardCrop.setClickable(loaded && choosable);
        binding.cardCrop.setFocusable(loaded && choosable);
        binding.ivCropMore.setVisibility(loaded && choosable ? View.VISIBLE : View.GONE);
        binding.ivCropMore.setImageResource(empty ? R.drawable.ic_chevron_right : R.drawable.ic_arrow_drop_down);
        if (c != null) {
            binding.ivCropIcon.setImageResource(c.icon != 0 ? c.icon : R.drawable.ic_nutrition);
            boolean hasVariety = c.variety != null && !c.variety.trim().isEmpty();
            binding.tvCropTitle.setText(hasVariety ? getString(R.string.add_record_crop_title, c.name, c.variety) : c.name);
            List<String> parts = new ArrayList<>();
            if (c.plotName != null && !c.plotName.isEmpty()) parts.add(c.plotName);
            if (c.stage != null && !c.stage.isEmpty()) parts.add(c.stage);
            if (c.daysPlanted > 0) parts.add(getString(R.string.add_record_crop_days, c.daysPlanted));
            binding.tvCropSub.setText(String.join(getString(R.string.add_record_crop_sep), parts));
            binding.tvCropSub.setVisibility(parts.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            binding.ivCropIcon.setImageResource(R.drawable.ic_nutrition);
            binding.tvCropTitle.setText(empty ? R.string.add_record_crop_empty : R.string.add_record_crop_choose);
            binding.tvCropSub.setText(empty ? R.string.add_record_crop_empty_desc : R.string.add_record_crop_choose_desc);
            binding.tvCropSub.setVisibility(loaded ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void onCropClicked() {
        List<CropCard> list = vm.crops.getValue();
        if (list == null) return;
        if (list.isEmpty()) {
            nav().navigate(R.id.addCropFragment);
            return;
        }
        String[] labels = new String[list.size()];
        int checked = -1;
        CropCard current = vm.crop.getValue();
        for (int i = 0; i < list.size(); i++) {
            CropCard c = list.get(i);
            boolean hasVariety = c.variety != null && !c.variety.trim().isEmpty();
            labels[i] = hasVariety ? getString(R.string.add_record_crop_title, c.name, c.variety) : c.name;
            if (current != null && current.id == c.id) checked = i;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_record_crop_choose)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    vm.selectCrop(list.get(which));
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------- 记录类型 ----------

    private void setUpTypes() {
        typeButtons = new MaterialButton[]{binding.type0, binding.type1, binding.type2,
                binding.type3, binding.type4, binding.type5};
        for (int i = 0; i < typeButtons.length; i++) {
            FarmType t = TYPES[i];
            MaterialButton b = typeButtons[i];
            b.setText(t.label);
            b.setIconResource(t.icon);
            b.setOnClickListener(v -> {
                vm.selectType(t);
                renderTypes(vm.type.getValue());
            });
        }
        vm.type.observe(getViewLifecycleOwner(), this::renderTypes);
    }

    private void renderTypes(@Nullable FarmType selected) {
        if (typeButtons == null) return;
        for (int i = 0; i < typeButtons.length; i++) typeButtons[i].setChecked(TYPES[i] == selected);
    }

    // ---------- 日期 · 时段 ----------

    private void setUpDate() {
        View.OnClickListener open = v -> showDatePicker();
        binding.etDate.setOnClickListener(open);
        binding.tilDate.setEndIconOnClickListener(open);
        vm.dateTime.observe(getViewLifecycleOwner(), dt -> {
            if (dt == null) return;
            String day = AddCropFormUi.date(requireContext(), dt.toLocalDate());
            binding.etDate.setText(getString(R.string.add_record_date_value, day,
                    getString(AddRecordFields.dayPartRes(dt.getHour()))));
        });
        reattachPickers();
    }

    private LocalDateTime currentDateTime() {
        LocalDateTime dt = vm.dateTime.getValue();
        return dt == null ? LocalDateTime.now() : dt;
    }

    private void showDatePicker() {
        if (getChildFragmentManager().findFragmentByTag(TAG_DATE) != null
                || getChildFragmentManager().findFragmentByTag(TAG_TIME) != null) return;
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.add_record_day_pick)
                .setSelection(AddCropHarvest.toPickerSelection(currentDateTime().toLocalDate()))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                        .setValidator(DateValidatorPointBackward.now())
                        .build())
                .build();
        attachDateListener(picker);
        picker.show(getChildFragmentManager(), TAG_DATE);
    }

    private void showTimePicker() {
        LocalDateTime dt = currentDateTime();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(dt.getHour())
                .setMinute(dt.getMinute())
                .setTitleText(R.string.add_record_time_pick)
                .build();
        attachTimeListener(picker);
        picker.show(getChildFragmentManager(), TAG_TIME);
    }

    /** 选好日期后立即生效（保留原来的时间），再弹出时间选择 */
    private void attachDateListener(MaterialDatePicker<Long> picker) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel == null) return;
            LocalDate day = AddCropHarvest.fromPickerSelection(sel);
            vm.setDateTime(day.atTime(currentDateTime().toLocalTime()));
            showTimePicker();
        });
    }

    private void attachTimeListener(MaterialTimePicker picker) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(v -> vm.setDateTime(
                currentDateTime().toLocalDate().atTime(LocalTime.of(picker.getHour(), picker.getMinute()))));
    }

    /** 旋转屏幕后对话框由系统恢复，重新挂上回调 */
    @SuppressWarnings("unchecked")
    private void reattachPickers() {
        Fragment date = getChildFragmentManager().findFragmentByTag(TAG_DATE);
        if (date instanceof MaterialDatePicker) attachDateListener((MaterialDatePicker<Long>) date);
        Fragment time = getChildFragmentManager().findFragmentByTag(TAG_TIME);
        if (time instanceof MaterialTimePicker) attachTimeListener((MaterialTimePicker) time);
    }

    // ---------- 随类型变化的字段 ----------

    private void setUpFields() {
        materialAdapter = new AddCropSuggestionAdapter(requireContext());
        binding.etMaterial.setAdapter(materialAdapter);
        binding.tilMaterial.setEndIconOnClickListener(v -> {
            if (materialAdapter.getCount() == 0 && binding.etMaterial.getText().length() == 0) return;
            materialAdapter.showAll();
            binding.etMaterial.requestFocus();
            binding.etMaterial.showDropDown();
        });
        binding.etUnit.setOnItemClickListener((parent, v, position, id) -> {
            AddRecordFields f = vm.fields.getValue();
            if (f != null && position >= 0 && position < f.units.size()) vm.unit.setValue(f.units.get(position));
        });
        vm.unit.observe(getViewLifecycleOwner(), u -> binding.etUnit.setText(u == null ? "" : u, false));
        vm.fields.observe(getViewLifecycleOwner(), this::renderFields);
    }

    private void renderFields(@Nullable AddRecordFields f) {
        if (f == null) return;
        materialLabel = f.materialLabel;
        amountLabel = f.amountLabel;
        binding.tilMaterial.setVisibility(f.hasMaterial() ? View.VISIBLE : View.GONE);
        if (f.hasMaterial()) binding.tilMaterial.setHint(f.materialLabel);
        List<AddCropSuggestionAdapter.Item> items = new ArrayList<>();
        if (f.suggestions != 0) {
            for (String s : getResources().getStringArray(f.suggestions)) {
                items.add(new AddCropSuggestionAdapter.Item(s, 0));
            }
        }
        materialAdapter.setItems(items);

        boolean amount = f.hasAmount();
        binding.tilAmount.setVisibility(amount ? View.VISIBLE : View.GONE);
        binding.tilUnit.setVisibility(amount ? View.VISIBLE : View.GONE);
        if (amount) {
            binding.tilAmount.setHint(f.amountLabel);
            binding.etUnit.setSimpleItems(f.units.toArray(new String[0]));
            String u = vm.unit.getValue();
            binding.etUnit.setText(u == null ? "" : u, false);
        }
        renderErrors();
        renderSync();
    }

    // ---------- 同步到收支记账 ----------

    private void setUpSync() {
        vm.sync.observe(getViewLifecycleOwner(), on -> {
            AddCropFormUi.renderSwitchIcon(binding.swSync, Boolean.TRUE.equals(on));
            renderSync();
        });
        vm.costCents.observe(getViewLifecycleOwner(), c -> renderSync());
    }

    /** 开：保存后自动记一笔支出 ¥60 · 化肥；未填费用：填写费用后自动记一笔支出；关：不记账 */
    private void renderSync() {
        if (!Boolean.TRUE.equals(vm.sync.getValue())) {
            binding.tvSync.setText(R.string.add_record_sync_off);
            return;
        }
        Long cents = vm.costCents.getValue();
        AddRecordFields f = vm.fields.getValue();
        if (cents == null || cents <= 0 || f == null) {
            binding.tvSync.setText(R.string.add_record_sync_no_cost);
        } else {
            binding.tvSync.setText(getString(R.string.add_record_sync_on, AddRecordMoney.formatYuan(cents),
                    f.ledgerCategory));
        }
    }

    // ---------- 现场照片 ----------

    private void setUpPhotos() {
        binding.btnAddPhoto.setOnClickListener(v -> {
            if (vm.remainingPhotos() <= 0) {
                showMessage(getString(R.string.add_record_photos_limit, AddRecordViewModel.MAX_PHOTOS));
                return;
            }
            pickPhotos.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
        vm.photos.observe(getViewLifecycleOwner(), this::renderPhotos);
    }

    private void renderPhotos(@Nullable List<String> list) {
        List<String> photos = list == null ? Collections.emptyList() : list;
        ViewGroup strip = binding.photoStrip;
        // 保留最后的「添加照片」方块，前面的照片重建
        while (strip.getChildCount() > 1) strip.removeViewAt(0);
        LayoutInflater inflater = LayoutInflater.from(strip.getContext());
        for (int i = 0; i < photos.size(); i++) {
            String path = photos.get(i);
            View item = inflater.inflate(R.layout.add_record_item_photo, strip, false);
            ImageView image = item.findViewById(R.id.image);
            image.setContentDescription(getString(R.string.add_record_photo_item, i + 1));
            BindingAdapters.loadImage(image, path,
                    ContextCompat.getDrawable(requireContext(), R.drawable.add_record_photo_placeholder));
            item.findViewById(R.id.btn_remove).setOnClickListener(v -> vm.removePhoto(path));
            strip.addView(item, i);
        }
        binding.btnAddPhoto.setVisibility(photos.size() < AddRecordViewModel.MAX_PHOTOS ? View.VISIBLE : View.GONE);
        String title = getString(R.string.add_record_photos);
        String count = getString(R.string.add_record_photos_count, photos.size(), AddRecordViewModel.MAX_PHOTOS);
        binding.tvPhotos.setText(photoTitle(title, count));
    }

    /** 「现场照片 2/9」：数字用常规字重、On Surface Variant */
    private CharSequence photoTitle(String title, String count) {
        SpannableStringBuilder sb = new SpannableStringBuilder(title).append(' ');
        int start = sb.length();
        sb.append(count);
        int color = MaterialColors.getColor(binding.tvPhotos, com.google.android.material.R.attr.colorOnSurfaceVariant);
        sb.setSpan(new ForegroundColorSpan(color), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new TypefaceSpan("sans-serif"), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    // ---------- 校验错误 ----------

    private void observeErrors() {
        vm.materialError.observe(getViewLifecycleOwner(), e -> renderErrors());
        vm.amountError.observe(getViewLifecycleOwner(), e -> renderErrors());
        vm.costError.observe(getViewLifecycleOwner(), res -> AddCropFormUi.renderError(binding.tilCost,
                res == null || res == 0 ? null : getString(res)));
    }

    private void renderErrors() {
        renderLabeledError(binding.tilMaterial, vm.materialError.getValue(), materialLabel);
        renderLabeledError(binding.tilAmount, vm.amountError.getValue(), amountLabel);
    }

    /** 「请填写肥料名称」「采收量需大于 0」：错误文字拼上当前字段名 */
    private void renderLabeledError(TextInputLayout til, @Nullable Integer res, @StringRes int label) {
        if (res == null || res == 0 || label == 0) {
            AddCropFormUi.renderError(til, null);
            return;
        }
        AddCropFormUi.renderError(til, getString(res, getString(label)));
    }
}
