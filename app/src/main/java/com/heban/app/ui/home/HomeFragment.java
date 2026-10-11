package com.heban.app.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.navigation.NavOptions;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.model.TaskItem;
import com.heban.app.databinding.FragmentHomeBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.LoginRequired;
import com.heban.app.ui.common.NavArgs;
import com.heban.app.ui.common.Snackbars;

/**
 * H01 首页：天气卡、预警栏、常用工具、今日农事、我的作物；下拉刷新天气。
 * 定位权限（技术准备文档表 4-1）：首页第一次加载天气时申请一次，拒绝则由数据层使用资料中的「所在地区」，不阻塞页面。
 */
public class HomeFragment extends BaseFragment {
    /** 是否已经申请过定位权限（只在首页第一次加载天气时申请一次） */
    static final Preferences.Key<Boolean> LOCATION_ASKED = PreferencesKeys.booleanKey("home.location_asked");

    private FragmentHomeBinding binding;
    private HomeViewModel vm;
    private HomeTaskAdapter taskAdapter;
    private HomeCropAdapter cropAdapter;

    private final ActivityResultLauncher<String> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && vm != null) vm.refresh();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(HomeViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setOnPickCity(v -> openCityPicker());
        binding.setOnAddTask(v -> {
            if (LoginRequired.check(this)) nav().navigate(R.id.addTaskFragment);
        });
        binding.setOnAddCrop(v -> {
            if (LoginRequired.check(this)) nav().navigate(R.id.addCropFragment);
        });

        setupHeader();
        setupLists();
        setupRefresh();

        binding.weatherCard.setOnClickListener(v -> nav().navigate(R.id.weatherFragment));
        binding.btnAlertView.setOnClickListener(v -> nav().navigate(R.id.weatherFragment));
        binding.toolScan.setOnClickListener(v -> nav().navigate(R.id.scan_graph));
        binding.toolCalendar.setOnClickListener(v -> nav().navigate(R.id.calendarFragment));
        binding.toolLedger.setOnClickListener(v -> {
            if (LoginRequired.check(this)) nav().navigate(R.id.addEntryFragment);
        });
        binding.toolCheckin.setOnClickListener(v -> nav().navigate(R.id.checkInFragment));
        binding.btnTasksAll.setOnClickListener(v -> nav().navigate(R.id.calendarFragment));
        binding.btnCropsAll.setOnClickListener(v -> navigateTopLevel(R.id.cropsFragment));

        // 空列表提交 null：之后的新列表直接插入（不在后台比较），列表与空状态切换不闪
        vm.state.observe(getViewLifecycleOwner(), s -> {
            taskAdapter.submitList(s.tasks.isEmpty() ? null : s.tasks);
            cropAdapter.submitList(s.crops.isEmpty() ? null : s.crops);
        });
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(view, msg));

        maybeAskLocation();
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.updateClock();
    }

    private void setupHeader() {
        binding.btnMessages.setOnClickListener(v -> nav().navigate(R.id.messagesFragment));
        binding.btnProfile.setOnClickListener(v -> navigateTopLevel(R.id.profileFragment));
    }

    private void setupLists() {
        taskAdapter = new HomeTaskAdapter(this::toggleTask);
        binding.tasksList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.tasksList.setAdapter(taskAdapter);
        RecyclerView.ItemAnimator anim = binding.tasksList.getItemAnimator();
        if (anim instanceof SimpleItemAnimator) ((SimpleItemAnimator) anim).setSupportsChangeAnimations(false);

        cropAdapter = new HomeCropAdapter(crop -> {
            Bundle args = new Bundle();
            args.putLong(NavArgs.CROP_ID, crop.id);
            nav().navigate(R.id.cropDetailFragment, args);
        });
        binding.cropsList.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.cropsList.setAdapter(cropAdapter);
        int gap = getResources().getDimensionPixelSize(R.dimen.card_gap);
        binding.cropsList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect out, @NonNull View child, @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                if (parent.getChildAdapterPosition(child) > 0) out.left = gap;
            }
        });
    }

    private void setupRefresh() {
        binding.swipe.setColorSchemeColors(MaterialColors.getColor(binding.swipe,
                androidx.appcompat.R.attr.colorPrimary));
        binding.swipe.setProgressBackgroundColorSchemeColor(MaterialColors.getColor(binding.swipe,
                com.google.android.material.R.attr.colorSurfaceContainerHigh));
        binding.swipe.setOnRefreshListener(vm::refresh);
        vm.refreshing.observe(getViewLifecycleOwner(), r -> binding.swipe.setRefreshing(Boolean.TRUE.equals(r)));
    }

    private void toggleTask(@NonNull TaskItem task) {
        if (!LoginRequired.check(this)) return;
        vm.setDone(task, !task.done);
    }

    private void openCityPicker() {
        if (getChildFragmentManager().findFragmentByTag(CityPickerDialog.TAG) == null) {
            new CityPickerDialog().show(getChildFragmentManager(), CityPickerDialog.TAG);
        }
    }

    /** 一级页面（作物、我的）与底部导航的切换方式一致：回到首页之上、保存并恢复各自的返回栈 */
    private void navigateTopLevel(@IdRes int dest) {
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(nav().getGraph().getStartDestinationId(), false, true)
                .build();
        nav().navigate(dest, null, options);
    }

    /** 首页第一次加载天气时申请定位（只申请一次）；授权后强制刷新天气 */
    private void maybeAskLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        SettingsStore settings = container().settings;
        if (settings.peek(LOCATION_ASKED, false)) return;
        // 写入很快，不随页面销毁取消
        //noinspection ResultOfMethodCallIgnored
        settings.put(LOCATION_ASKED, true).onErrorComplete().subscribe();
        locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
