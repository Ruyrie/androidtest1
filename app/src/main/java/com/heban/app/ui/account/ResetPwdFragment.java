package com.heban.app.ui.account;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;

import com.heban.app.HeBanApp;
import com.heban.app.R;
import com.heban.app.databinding.FragmentResetPwdBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.Snackbars;
import com.heban.app.util.PasswordStrength;

/** A08 设置新密码：新密码 + 确认 + 规则校验；成功后在同一页显示「密码已重置」，去登录回到 A03。 */
public class ResetPwdFragment extends BaseFragment {
    private FragmentResetPwdBinding binding;
    private ResetPwdViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResetPwdBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(nav().getBackStackEntry(R.id.reset_graph),
                HeBanApp.factory(requireContext())).get(ResetPwdViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> {
            if (!Boolean.TRUE.equals(vm.resetting.getValue())) nav().navigateUp();
        });
        binding.btnLogin.setOnClickListener(v -> goLogin());
        binding.tilConfirmPwd.setEndIconCheckable(false);
        binding.tilConfirmPwd.setEndIconContentDescription(R.string.reset_confirm_match);

        // 提交中忽略返回键；成功状态下按返回键同样回到登录页（重置流程已结束）
        OnBackPressedCallback back = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (Boolean.TRUE.equals(vm.done.getValue())) goLogin();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
        Observer<Boolean> updateBack = x -> back.setEnabled(
                Boolean.TRUE.equals(vm.done.getValue()) || Boolean.TRUE.equals(vm.resetting.getValue()));
        vm.done.observe(getViewLifecycleOwner(), updateBack);
        vm.strength.observe(getViewLifecycleOwner(), this::renderStrength);
        vm.confirm.observe(getViewLifecycleOwner(), this::renderConfirm);
        vm.lengthRule.observe(getViewLifecycleOwner(), r ->
                ResetUi.applyRule(binding.icRuleLength, binding.tvRuleLength, r));
        vm.mixRule.observe(getViewLifecycleOwner(), r ->
                ResetUi.applyRule(binding.icRuleMix, binding.tvRuleMix, r));
        vm.recentRule.observe(getViewLifecycleOwner(), r ->
                ResetUi.applyRule(binding.icRuleRecent, binding.tvRuleRecent, r));
        vm.resetting.observe(getViewLifecycleOwner(), loading -> {
            ResetUi.setLoading(binding.btnFinish, Boolean.TRUE.equals(loading));
            updateBack.onChanged(loading);
        });
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(view, msg));
    }

    /** 强度条：未输入全灰；弱 1 格 Error；中 2 格 Warning；强 3 格 Primary */
    private void renderStrength(PasswordStrength.Result r) {
        int lit = ResetPwdViewModel.strengthSegments(r);
        int litAttr;
        switch (lit) {
            case 1: litAttr = androidx.appcompat.R.attr.colorError; break;
            case 2: litAttr = R.attr.colorWarning; break;
            case 3: litAttr = androidx.appcompat.R.attr.colorPrimary; break;
            default: litAttr = com.google.android.material.R.attr.colorOnSurfaceVariant; break;
        }
        int litColor = ResetUi.color(binding.seg1, litAttr);
        int offColor = ResetUi.color(binding.seg1, com.google.android.material.R.attr.colorSurfaceContainerHighest);
        View[] segs = {binding.seg1, binding.seg2, binding.seg3};
        for (int i = 0; i < segs.length; i++) {
            segs[i].setBackgroundTintList(ColorStateList.valueOf(i < lit ? litColor : offColor));
        }
        binding.tvStrength.setTextColor(litColor);
    }

    /** 确认密码：未输入为提示；一致时右侧显示对勾、提示为 Primary；不一致时为错误状态 */
    private void renderConfirm(ResetPwdViewModel.Confirm state) {
        if (state == ResetPwdViewModel.Confirm.MISMATCH) {
            binding.tilConfirmPwd.setEndIconVisible(false);
            binding.tilConfirmPwd.setError(getString(R.string.reset_confirm_mismatch));
            return;
        }
        binding.tilConfirmPwd.setError(null);
        binding.tilConfirmPwd.setErrorEnabled(false);
        boolean match = state == ResetPwdViewModel.Confirm.MATCH;
        binding.tilConfirmPwd.setEndIconVisible(match);
        binding.tilConfirmPwd.setHelperText(getString(match ? R.string.reset_confirm_match : R.string.reset_confirm_hint));
        binding.tilConfirmPwd.setHelperTextColor(ColorStateList.valueOf(ResetUi.color(binding.tilConfirmPwd,
                match ? androidx.appcompat.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorOnSurfaceVariant)));
    }

    /** 去登录：回到 A03 并移除整个找回密码流程 */
    private void goLogin() {
        nav().navigate(R.id.loginFragment, null, new NavOptions.Builder()
                .setPopUpTo(R.id.reset_graph, true)
                .setLaunchSingleTop(true)
                .build());
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
