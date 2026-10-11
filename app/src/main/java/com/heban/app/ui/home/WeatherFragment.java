package com.heban.app.ui.home;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.databinding.FragmentWeatherBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.Snackbars;

import java.util.Collections;

/**
 * H02 天气详情 · 农事指数（技术准备文档 12.2）：预警、当前天气与逐小时、今日农事指数 3×2、未来 7 天；
 * 点击定位图标通过城市搜索切换地点；下拉刷新。
 */
public class WeatherFragment extends BaseFragment {
    private static final int INDEX_COLUMNS = 3;

    private FragmentWeatherBinding binding;
    private WeatherViewModel vm;
    private final WeatherHourlyAdapter hourlyAdapter = new WeatherHourlyAdapter();
    private final WeatherIndexAdapter indexAdapter = new WeatherIndexAdapter();
    private final WeatherDailyAdapter dailyAdapter = new WeatherDailyAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(WeatherViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setOnPickCity(v -> openCityPicker());
        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());

        binding.hourly.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.hourly.setAdapter(hourlyAdapter);

        int gap = getResources().getDimensionPixelSize(com.heban.app.R.dimen.chip_gap);
        binding.indices.setLayoutManager(new GridLayoutManager(requireContext(), INDEX_COLUMNS));
        binding.indices.addItemDecoration(new GridGap(INDEX_COLUMNS, gap));
        binding.indices.setAdapter(indexAdapter);

        binding.daily.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.daily.setAdapter(dailyAdapter);

        binding.swipe.setColorSchemeColors(MaterialColors.getColor(binding.swipe,
                androidx.appcompat.R.attr.colorPrimary));
        binding.swipe.setProgressBackgroundColorSchemeColor(MaterialColors.getColor(binding.swipe,
                com.google.android.material.R.attr.colorSurfaceContainerHigh));
        binding.swipe.setOnRefreshListener(vm::refresh);
        vm.refreshing.observe(getViewLifecycleOwner(), r -> binding.swipe.setRefreshing(Boolean.TRUE.equals(r)));

        vm.state.observe(getViewLifecycleOwner(), this::render);
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(view, msg));
    }

    private void render(WeatherStateUi ui) {
        long now = WeatherFormat.currentTimeMillis();
        WeatherBundle b = ui.bundle();
        hourlyAdapter.submit(WeatherFormat.hours(b, now));
        indexAdapter.submit(ui.hasData() ? ui.state.indices : Collections.emptyList());
        dailyAdapter.submit(b != null && b.daily != null ? b.daily : Collections.emptyList(), now);
    }

    private void openCityPicker() {
        if (getChildFragmentManager().findFragmentByTag(CityPickerDialog.TAG) == null) {
            new CityPickerDialog().show(getChildFragmentManager(), CityPickerDialog.TAG);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    /** 宫格间距 8dp（列间、行间） */
    private static final class GridGap extends RecyclerView.ItemDecoration {
        private final int columns;
        private final int gap;

        GridGap(int columns, int gap) {
            this.columns = columns;
            this.gap = gap;
        }

        @Override
        public void getItemOffsets(@NonNull Rect out, @NonNull View child, @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(child);
            if (pos == RecyclerView.NO_POSITION) return;
            int col = pos % columns;
            out.left = col * gap / columns;
            out.right = gap - (col + 1) * gap / columns;
            if (pos >= columns) out.top = gap;
        }
    }
}
