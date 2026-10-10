package com.heban.app.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDestination;

import com.heban.app.HeBanApp;
import com.heban.app.R;
import com.heban.app.data.remote.sms.FakeSmsService;
import com.heban.app.databinding.FragmentForgotPwdBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.Snackbars;

/** A07 忘记密码 · 验证手机：手机号 + 短信验证码，通过后进入 A08 设置新密码。 */
public class ForgotPwdFragment extends BaseFragment {
    private FragmentForgotPwdBinding binding;
    private ResetPwdViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentForgotPwdBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(nav().getBackStackEntry(R.id.reset_graph),
                HeBanApp.factory(requireContext())).get(ResetPwdViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        ResetUi.decoratePhonePrefix(binding.tilPhone);
        ResetUi.attachInlineAction(binding.tilCode, binding.codeAction);
        binding.etPhone.addTextChangedListener(new ResetUi.PhoneSpacingWatcher());
        binding.cardAppeal.setOnClickListener(v -> nav().navigate(R.id.phoneAppealFragment));

        vm.verifying.observe(getViewLifecycleOwner(), loading ->
                ResetUi.setLoading(binding.btnNext, Boolean.TRUE.equals(loading)));
        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(view, msg));
        vm.fakeSmsSent.observe(getViewLifecycleOwner(), sent ->
                Snackbars.show(view, getString(R.string.fake_sms_hint, FakeSmsService.CODE)));
        vm.toResetPwd.observe(getViewLifecycleOwner(), go -> {
            NavDestination cur = nav().getCurrentDestination();
            if (cur != null && cur.getId() == R.id.forgotPwdFragment) {
                nav().navigate(R.id.resetPwdFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
