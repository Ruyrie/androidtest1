package com.heban.app.ui.account;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.remote.sms.FakeSmsService;
import com.heban.app.databinding.FragmentLoginSmsBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.NavRoutes;
import com.heban.app.ui.common.Snackbars;

/** A04 验证码登录（原型 LoginSms.dc.html） */
public class LoginSmsFragment extends BaseFragment {
    /** 可选：从密码登录页带过来的手机号（11 位以内的数字） */
    public static final String ARG_PHONE = "phone";
    /** 可选：密码登录页上协议的勾选状态 */
    public static final String ARG_AGREED = "agreed";

    private FragmentLoginSmsBinding binding;
    private LoginSmsViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginSmsBinding.inflate(inflater, container, false);
        vm = viewModel(LoginSmsViewModel.class);
        binding.setVm(vm);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setOpenAgreement(v -> navigate(R.id.aboutFragment));
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_PHONE)) {
            vm.prefillOnce(args.getString(ARG_PHONE), args.getBoolean(ARG_AGREED));
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LoginPhoneInput.install(binding.phone.tilPhone, binding.phone.etPhone);

        binding.tabs.tabPassword.setOnClickListener(v -> backToPasswordLogin());
        binding.btnLogin.setOnClickListener(v -> submit());
        binding.etCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            if (binding.btnLogin.isEnabled()) submit();
            else hideKeyboard();
            return true;
        });
        setUpHelpText();

        vm.messageRes.observe(getViewLifecycleOwner(), res -> Snackbars.show(view, getString(res)));
        vm.toast.observe(getViewLifecycleOwner(), text -> Snackbars.show(view, text));
        vm.fakeSmsSent.observe(getViewLifecycleOwner(), sent ->
                Snackbars.show(view, getString(R.string.fake_sms_hint, FakeSmsService.CODE)));
        vm.goHome.observe(getViewLifecycleOwner(), go -> NavRoutes.enterHome(nav()));
    }

    /** 「请确认号码正确、短信未被拦截；也可以改用 密码登录」，「密码登录」可点击 */
    private void setUpHelpText() {
        String link = getString(R.string.login_tab_password);
        String full = getString(R.string.login_sms_help, link);
        int start = full.lastIndexOf(link);
        int linkColor = MaterialColors.getColor(binding.tvHelp, androidx.appcompat.R.attr.colorPrimary);
        SpannableString text = new SpannableString(full);
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                backToPasswordLogin();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(linkColor);
                ds.setUnderlineText(false);
            }
        }, start, start + link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL)),
                start, start + link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.tvHelp.setText(text);
        binding.tvHelp.setMovementMethod(LinkMovementMethod.getInstance());
        binding.tvHelp.setHighlightColor(Color.TRANSPARENT);
    }

    /** 回到密码登录：从密码登录页过来的直接返回（带回手机号和勾选状态），否则替换为密码登录页 */
    private void backToPasswordLogin() {
        NavDestination current = nav().getCurrentDestination();
        if (current == null || current.getId() != R.id.loginSmsFragment) return;
        NavBackStackEntry previous = nav().getPreviousBackStackEntry();
        if (previous != null && previous.getDestination().getId() == R.id.loginFragment) {
            previous.getSavedStateHandle().set(LoginFragment.RESULT_AGREED, Boolean.TRUE.equals(vm.agreed.getValue()));
            previous.getSavedStateHandle().set(LoginFragment.RESULT_PHONE, LoginPhoneInput.digits(vm.phone.getValue()));
            nav().popBackStack();
        } else {
            nav().navigate(R.id.loginFragment, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.loginSmsFragment, true)
                    .build());
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
    private void navigate(@IdRes int dest) {
        NavDestination current = nav().getCurrentDestination();
        if (current == null || current.getId() != R.id.loginSmsFragment) return;
        nav().navigate(dest);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
