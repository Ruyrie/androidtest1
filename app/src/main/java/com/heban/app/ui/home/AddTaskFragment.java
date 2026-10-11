package com.heban.app.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.heban.app.R;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.FarmType;
import com.heban.app.databinding.FragmentAddTaskBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * H04 新建农事。参数（均可选）：{@link NavArgs#FARM_TYPE}（FarmType 名称，默认用药）、
 * {@link NavArgs#EPOCH_DAY}（默认明天）、{@link NavArgs#CROP_ID}、{@link NavArgs#TASK_TITLE}。
 * 第一次打开「提醒我」（或保存时提醒开启且尚未授权）申请 POST_NOTIFICATIONS。
 */
public class AddTaskFragment extends BaseFragment {
    private static final String TAG_DATE_PICKER = "add_task_date_picker";
    private static final String TAG_TIME_PICKER = "add_task_time_picker";
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private FragmentAddTaskBinding binding;
    private AddTaskViewModel vm;
    private final List<MaterialCardView> typeCards = new ArrayList<>();
    private List<CropCard> cropOptions = Collections.emptyList();
    /** 本页已经申请过通知权限（避免重复弹窗） */
    private boolean askedNotification;
    /** 权限结果返回后继续保存 */
    private boolean saveAfterPermission;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (binding == null) return;
                if (!Boolean.TRUE.equals(granted)) showNotificationDenied();
                if (saveAfterPermission) {
                    saveAfterPermission = false;
                    vm.save();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(AddTaskViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.btnSave.setOnClickListener(v -> onSaveClicked());

        setUpTypes();
        setUpCrop();
        setUpDateTime();
        setUpRepeat();
        binding.rowRemind.setOnClickListener(v -> binding.swRemind.toggle());
        binding.swRemind.setOnCheckedChangeListener((button, checked) -> {
            if (checked == Boolean.TRUE.equals(vm.remind.getValue())) return;   // 来自数据绑定
            vm.setRemind(checked);
            if (checked) requestNotificationIfNeeded(false);
        });

        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
        vm.saved.observe(getViewLifecycleOwner(), id -> {
            Snackbars.show(requireActivity().findViewById(android.R.id.content), getString(R.string.add_task_saved));
            nav().navigateUp();
        });

        Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        Long epochDay = args.containsKey(NavArgs.EPOCH_DAY) ? args.getLong(NavArgs.EPOCH_DAY) : null;
        vm.start(args.getString(NavArgs.FARM_TYPE), epochDay, args.getLong(NavArgs.CROP_ID, 0L),
                args.getString(NavArgs.TASK_TITLE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        typeCards.clear();
        binding = null;
    }

    // ---------- 农事类型 ----------

    private void setUpTypes() {
        FarmType[] types = FarmType.values();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gap = Math.round(8 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < types.length; i++) {
            FarmType t = types[i];
            LinearLayout row = i < 3 ? binding.typeRow1 : binding.typeRow2;
            MaterialCardView card = (MaterialCardView) inflater.inflate(R.layout.add_task_item_type, row, false);
            ((ImageView) card.findViewById(R.id.iv_icon)).setImageResource(t.icon);
            ((TextView) card.findViewById(R.id.tv_label)).setText(t.label);
            card.setTag(t);
            card.setContentDescription(t.label);
            card.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(RadioButton.class.getName());
                    info.setCheckable(true);
                    info.setChecked(t == vm.type.getValue());
                }
            });
            card.setOnClickListener(v -> vm.selectType(t));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
            if (i % 3 != 0) lp.setMarginStart(gap);
            row.addView(card, lp);
            typeCards.add(card);
        }
        vm.type.observe(getViewLifecycleOwner(), this::renderTypes);
    }

    private void renderTypes(@Nullable FarmType selected) {
        for (MaterialCardView card : typeCards) {
            boolean on = card.getTag() == selected;
            int bg = color(on ? com.google.android.material.R.attr.colorSecondaryContainer
                    : com.google.android.material.R.attr.colorSurfaceContainerLow);
            int fg = color(on ? com.google.android.material.R.attr.colorOnSecondaryContainer
                    : com.google.android.material.R.attr.colorOnSurfaceVariant);
            card.setCardBackgroundColor(bg);
            card.setStrokeColor(on ? color(androidx.appcompat.R.attr.colorPrimary)
                    : android.graphics.Color.TRANSPARENT);
            ((ImageView) card.findViewById(R.id.iv_icon)).setImageTintList(ColorStateList.valueOf(fg));
            ((TextView) card.findViewById(R.id.tv_label)).setTextColor(fg);
            card.setSelected(on);
        }
    }

    // ---------- 关联作物 ----------

    private void setUpCrop() {
        binding.etCrop.setOnItemClickListener((parent, v, position, id) ->
                vm.selectCrop(position < cropOptions.size() ? cropOptions.get(position) : null));
        vm.cropOptions.observe(getViewLifecycleOwner(), list -> {
            cropOptions = list == null ? Collections.emptyList() : list;
            String[] items = new String[cropOptions.size() + 1];
            for (int i = 0; i < cropOptions.size(); i++) items[i] = cropText(cropOptions.get(i));
            items[cropOptions.size()] = getString(R.string.add_task_crop_none);
            binding.etCrop.setSimpleItems(items);
            renderCrop(vm.crop.getValue());
        });
        vm.crop.observe(getViewLifecycleOwner(), this::renderCrop);
    }

    private void renderCrop(@Nullable CropCard crop) {
        binding.etCrop.setText(crop == null ? getString(R.string.add_task_crop_none) : cropText(crop), false);
        binding.tilCrop.setStartIconDrawable(small(crop != null && crop.icon != 0 ? crop.icon : R.drawable.ic_nutrition));
    }

    /** 「番茄 · 1号大棚」 */
    private String cropText(CropCard c) {
        String name = c.name == null ? "" : c.name;
        return c.plotName == null || c.plotName.isEmpty() ? name
                : getString(R.string.add_task_crop_with_plot, name, c.plotName);
    }

    /** 关联作物框左侧图标按原型显示为 20dp */
    private Drawable small(@DrawableRes int res) {
        Drawable d = ContextCompat.getDrawable(requireContext(), res);
        if (d == null) return null;
        int px = Math.round(20 * getResources().getDisplayMetrics().density);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{d});
        ld.setLayerSize(0, px, px);
        return ld;
    }

    // ---------- 日期 / 时间 ----------

    private void setUpDateTime() {
        binding.etDate.setOnClickListener(v -> openDatePicker());
        binding.tilDate.setEndIconOnClickListener(v -> openDatePicker());
        binding.etTime.setOnClickListener(v -> openTimePicker());
        binding.tilTime.setEndIconOnClickListener(v -> openTimePicker());
        vm.date.observe(getViewLifecycleOwner(), d -> {
            if (d != null) binding.etDate.setText(dateText(d));
        });
        vm.time.observe(getViewLifecycleOwner(), t -> {
            if (t != null) binding.etTime.setText(t.format(HM));
        });
    }

    /** 「10月11日 周日」 */
    private String dateText(LocalDate d) {
        String[] weekdays = getResources().getStringArray(R.array.calendar_weekday_names);
        return getString(R.string.calendar_day_title, d.getMonthValue(), d.getDayOfMonth(),
                weekdays[CalendarDates.weekdayIndex(d)]);
    }

    private void openDatePicker() {
        if (getChildFragmentManager().findFragmentByTag(TAG_DATE_PICKER) != null) return;
        LocalDate current = vm.date.getValue() != null ? vm.date.getValue() : LocalDate.now().plusDays(1);
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.add_task_pick_date)
                .setSelection(CalendarDates.toPickerSelection(current))
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel != null) vm.selectDate(CalendarDates.fromPickerSelection(sel));
        });
        picker.show(getChildFragmentManager(), TAG_DATE_PICKER);
    }

    private void openTimePicker() {
        if (getChildFragmentManager().findFragmentByTag(TAG_TIME_PICKER) != null) return;
        LocalTime current = vm.time.getValue() != null ? vm.time.getValue() : AddTaskViewModel.DEFAULT_TIME;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(current.getHour())
                .setMinute(current.getMinute())
                .setTitleText(R.string.add_task_pick_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> vm.selectTime(picker.getHour(), picker.getMinute()));
        picker.show(getChildFragmentManager(), TAG_TIME_PICKER);
    }

    // ---------- 重复 ----------

    private void setUpRepeat() {
        int n = AddTaskViewModel.REPEAT_EVERY_DAYS.length;
        String[] items = new String[n];
        for (int i = 0; i < n; i++) items[i] = repeatText(i);
        binding.etRepeat.setSimpleItems(items);
        binding.etRepeat.setOnItemClickListener((parent, v, position, id) -> vm.selectRepeat(position));
        vm.repeatIndex.observe(getViewLifecycleOwner(), idx ->
                binding.etRepeat.setText(repeatText(idx == null ? 0 : idx), false));
    }

    private String repeatText(int index) {
        int every = AddTaskViewModel.REPEAT_EVERY_DAYS[index];
        if (every <= 0) return getString(R.string.add_task_repeat_none);
        return getString(R.string.add_task_repeat_every, every, AddTaskViewModel.REPEAT_TIMES[index]);
    }

    // ---------- 提醒权限 / 保存 ----------

    private boolean needsNotificationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    /** 申请通知权限（本页只申请一次）；返回 true 表示已发起申请 */
    private boolean requestNotificationIfNeeded(boolean thenSave) {
        if (!needsNotificationPermission() || askedNotification) return false;
        askedNotification = true;
        saveAfterPermission = thenSave;
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        return true;
    }

    private void showNotificationDenied() {
        Snackbars.show(binding.getRoot(), getString(R.string.add_task_notify_denied),
                getString(R.string.add_task_notify_settings), v -> {
                    Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
                    try {
                        startActivity(i);
                    } catch (RuntimeException ignored) {
                        // 没有系统设置页时忽略
                    }
                });
    }

    private void onSaveClicked() {
        if (!LoginRequired.check(this)) return;
        if (Boolean.TRUE.equals(vm.remind.getValue()) && requestNotificationIfNeeded(true)) return;
        vm.save();
    }

    private int color(int attr) {
        return MaterialColors.getColor(binding.getRoot(), attr);
    }
}
