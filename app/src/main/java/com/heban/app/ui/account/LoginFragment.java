package com.heban.app.ui.account;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.SavedStateHandle;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavDestination;

import com.heban.app.R;
import com.heban.app.databinding.FragmentLoginBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.NavRoutes;
import com.heban.app.ui.common.Snackbars;

/** A03 密码登录（原型 Login.dc.html） */
public class LoginFragment extends BaseFragment {
    /** 从验证码登录页返回时带回的手机号和勾选状态（SavedStateHandle） */
    static final String RESULT_PHONE = "login.phone";
    static final String RESULT_AGREED = "login.agreed";

    private FragmentLoginBinding binding;
    private LoginViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        vm = viewModel(LoginViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setOpenAgreement(v -> navigate(R.id.aboutFragment, null));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LoginPhoneInput.install(binding.phone.tilPhone, binding.phone.etPhone);

        binding.tabs.tabSms.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(LoginSmsFragment.ARG_PHONE, LoginPhoneInput.digits(vm.phone.getValue()));
            args.putBoolean(LoginSmsFragment.ARG_AGREED, Boolean.TRUE.equals(vm.agreed.getValue()));
            navigate(R.id.loginSmsFragment, args);
        });
        binding.btnForgot.setOnClickListener(v -> navigate(R.id.reset_graph, null));
        binding.linkRegister.setOnClickListener(v -> navigate(R.id.register_graph, null));
        binding.btnLogin.setOnClickListener(v -> submit());
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            if (binding.btnLogin.isEnabled()) submit();
            else hideKeyboard();
            return true;
        });

        vm.messageRes.observe(getViewLifecycleOwner(), res -> Snackbars.show(view, getString(res)));
        vm.toast.observe(getViewLifecycleOwner(), text -> Snackbars.show(view, text));
        vm.goHome.observe(getViewLifecycleOwner(), go -> NavRoutes.enterHome(nav()));

        // 从验证码登录页返回时，同步对方页面上输入的手机号和勾选状态
        NavBackStackEntry entry = nav().getCurrentBackStackEntry();
        if (entry != null && entry.getDestination().getId() == R.id.loginFragment) {
            SavedStateHandle state = entry.getSavedStateHandle();
            state.<String>getLiveData(RESULT_PHONE).observe(getViewLifecycleOwner(), p -> {
                if (p == null) return;
                Boolean agreed = state.get(RESULT_AGREED);
                vm.applyFromOtherTab(p, Boolean.TRUE.equals(agreed));
                state.remove(RESULT_PHONE);
                state.remove(RESULT_AGREED);
            });
        }
    }

    private void submit() {
        hideKeyboard();
        vm.login();
    }

    private void hideKeyboard() {
        View focus = requireActivity().getCurrentFocus();
        if (focus == null) return;
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        focus.clearFocus();
    }

    /** 只在本页仍是当前页时跳转，避免连点重复入栈 */
    private void navigate(@IdRes int dest, @Nullable Bundle args) {
        NavDestination current = nav().getCurrentDestination();
        if (current == null || current.getId() != R.id.loginFragment) return;
        nav().navigate(dest, args);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
