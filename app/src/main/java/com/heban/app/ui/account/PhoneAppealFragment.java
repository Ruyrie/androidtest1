package com.heban.app.ui.account;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavOptions;

import com.heban.app.R;
import com.heban.app.data.remote.sms.FakeSmsService;
import com.heban.app.databinding.FragmentPhoneAppealBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.Snackbars;

/**
 * A09 手机号已停用 · 申诉换绑：同一页面内三步。
 * 第 2 步按返回键 / 顶栏返回回到第 1 步；第 3 步（已提交）的返回键与「返回登录」一致。
 */
public class PhoneAppealFragment extends BaseFragment {
    private FragmentPhoneAppealBinding binding;
    private PhoneAppealViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPhoneAppealBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = viewModel(PhoneAppealViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        ResetUi.decoratePhonePrefix(binding.tilOldPhone);
        ResetUi.decoratePhonePrefix(binding.tilNewPhone);
        ResetUi.attachInlineAction(binding.tilCode, binding.codeAction);
        setupDropdowns();

        OnBackPressedCallback stepBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                onBack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), stepBack);
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (!vm.back()) nav().navigateUp();
        });
        binding.btnBackLogin.setOnClickListener(v -> backToLogin());

        vm.step.observe(getViewLifecycleOwner(), step -> {
            stepBack.setEnabled(step != null && step != PhoneAppealViewModel.STEP_ACCOUNT);
            binding.scroll.scrollTo(0, 0);
        });
        vm.samePhone.observe(getViewLifecycleOwner(), same -> binding.tilCode.setHelperTextColor(
                ColorStateList.valueOf(ResetUi.color(binding.tilCode, Boolean.TRUE.equals(same)
                        ? androidx.appcompat.R.attr.colorError
                        : com.google.android.material.R.attr.colorOnSurfaceVariant))));
        vm.submitting.observe(getViewLifecycleOwner(), loading ->
                ResetUi.setLoading(binding.btnSubmit, Boolean.TRUE.equals(loading)));
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(view, msg));
        vm.fakeSmsSent.observe(getViewLifecycleOwner(), sent ->
                Snackbars.show(view, getString(R.string.fake_sms_hint, FakeSmsService.CODE)));
    }

    /** 「最近一次登录」「常用登录地区」：Exposed Dropdown，不可手动输入 */
    private void setupDropdowns() {
        binding.actLastLogin.setText(vm.lastLogin.getValue(), false);
        binding.actLastLogin.setOnItemClickListener((parent, v, position, id) ->
                vm.lastLogin.setValue(String.valueOf(parent.getItemAtPosition(position))));
        binding.actRegion.setText(vm.region.getValue(), false);
        binding.actRegion.setOnItemClickListener((parent, v, position, id) ->
                vm.region.setValue(String.valueOf(parent.getItemAtPosition(position))));
    }

    private void onBack() {
        if (vm.back()) return;
        Integer step = vm.step.getValue();
        if (step != null && step == PhoneAppealViewModel.STEP_SUBMITTED) backToLogin();
    }

    /** 返回登录：回到 A03，并移除登录页之上的找回密码 / 申诉页面 */
    private void backToLogin() {
        nav().navigate(R.id.loginFragment, null, new NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, false)
                .setLaunchSingleTop(true)
                .build());
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
