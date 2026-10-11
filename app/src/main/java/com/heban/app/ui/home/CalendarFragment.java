package com.heban.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.model.TaskItem;
import com.heban.app.databinding.FragmentCalendarBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * H03 农事日历。参数：可选 {@link NavArgs#EPOCH_DAY}（long，打开某一天，缺省为今天）。
 * 「新建农事」打开 H04 并带上所选日期。
 */
public class CalendarFragment extends BaseFragment {
    private static final String TAG_DATE_PICKER = "calendar_date_picker";

    private FragmentCalendarBinding binding;
    private CalendarViewModel vm;
    private CalendarDayAdapter dayAdapter;
    private CalendarTaskAdapter taskAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(CalendarViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.btnMonth.setOnClickListener(v -> openDatePicker());

        dayAdapter = new CalendarDayAdapter(vm::onDayClicked);
        binding.grid.setLayoutManager(new GridLayoutManager(requireContext(), CalendarDates.COLUMNS));
        binding.grid.setItemAnimator(null);
        binding.grid.setAdapter(dayAdapter);

        taskAdapter = new CalendarTaskAdapter(new CalendarTaskAdapter.Listener() {
            @Override
            public void onToggle(@NonNull TaskItem task) {
                if (!LoginRequired.check(CalendarFragment.this)) return;
                vm.toggleDone(task);
            }

            @Override
            public void onLongPress(@NonNull TaskItem task) {
                confirmDelete(task);
            }
        });
        binding.taskList.setLayoutManager(new LinearLayoutManager(requireContext()));
        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(requireContext(),
                LinearLayoutManager.VERTICAL);
        divider.setDividerColor(MaterialColors.getColor(binding.taskList,
                com.google.android.material.R.attr.colorSurfaceVariant));
        divider.setDividerThickness(Math.max(1, Math.round(getResources().getDisplayMetrics().density)));
        divider.setLastItemDecorated(false);
        binding.taskList.addItemDecoration(divider);
        binding.taskList.setAdapter(taskAdapter);

        vm.month.observe(getViewLifecycleOwner(), this::renderMonth);
        vm.days.observe(getViewLifecycleOwner(), days -> dayAdapter.submitList(days));
        vm.selected.observe(getViewLifecycleOwner(), this::renderSelectedDay);
        vm.dayTasks.observe(getViewLifecycleOwner(), this::renderTasks);
        vm.upcomingTerm.observe(getViewLifecycleOwner(), this::renderTerm);
        vm.openAddTask.observe(getViewLifecycleOwner(), this::openAddTask);
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
        vm.toastRes.observe(getViewLifecycleOwner(), res -> Snackbars.show(binding.getRoot(), getString(res)));

        Bundle args = getArguments();
        Long epochDay = args != null && args.containsKey(NavArgs.EPOCH_DAY) ? args.getLong(NavArgs.EPOCH_DAY) : null;
        vm.start(CalendarDates.fromEpochDay(epochDay));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        dayAdapter = null;
        taskAdapter = null;
    }

    private void renderMonth(@Nullable YearMonth ym) {
        if (ym == null) return;
        binding.tvMonth.setText(getString(R.string.calendar_month_title, ym.getYear(), ym.getMonthValue()));
    }

    private void renderSelectedDay(@Nullable LocalDate day) {
        if (day == null) return;
        binding.tvDay.setText(dayTitle(day));
    }

    private void renderTasks(@Nullable List<TaskItem> tasks) {
        boolean has = tasks != null && !tasks.isEmpty();
        binding.cardTasks.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.tvCount.setVisibility(has ? View.VISIBLE : View.GONE);
        if (has) binding.tvCount.setText(getString(R.string.calendar_day_count, tasks.size()));
        taskAdapter.submitList(tasks);
    }

    private void renderTerm(@Nullable SolarTerms.Term term) {
        boolean show = term != null && !term.tip.isEmpty();
        binding.termTip.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            binding.tvTermTip.setText(getString(R.string.calendar_term_tip, term.name,
                    term.date.getMonthValue(), term.date.getDayOfMonth(), term.tip));
        }
    }

    /** 「10月10日 周六」 */
    private String dayTitle(LocalDate day) {
        String[] weekdays = getResources().getStringArray(R.array.calendar_weekday_names);
        return getString(R.string.calendar_day_title, day.getMonthValue(), day.getDayOfMonth(),
                weekdays[CalendarDates.weekdayIndex(day)]);
    }

    /** 点月份：用日期选择器跳到任意一天 */
    private void openDatePicker() {
        if (getChildFragmentManager().findFragmentByTag(TAG_DATE_PICKER) != null) return;
        LocalDate current = vm.selected.getValue() != null ? vm.selected.getValue() : vm.today();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.calendar_pick_date)
                .setSelection(CalendarDates.toPickerSelection(current))
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel != null) vm.select(CalendarDates.fromPickerSelection(sel));
        });
        picker.show(getChildFragmentManager(), TAG_DATE_PICKER);
    }

    private void openAddTask(@Nullable LocalDate day) {
        if (day == null) return;
        if (!LoginRequired.check(this)) return;
        Bundle args = new Bundle();
        args.putLong(NavArgs.EPOCH_DAY, day.toEpochDay());
        nav().navigate(R.id.addTaskFragment, args);
    }

    private void confirmDelete(@NonNull TaskItem task) {
        if (!LoginRequired.check(this)) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.calendar_delete_title)
                .setMessage(getString(R.string.calendar_delete_message, task.title == null ? "" : task.title))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> vm.delete(task))
                .show();
    }
}
