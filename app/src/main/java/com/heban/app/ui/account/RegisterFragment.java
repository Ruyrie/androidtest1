package com.heban.app.ui.account;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.heban.app.HeBanApp;
import com.heban.app.R;
import com.heban.app.databinding.FragmentRegisterBinding;
import com.heban.app.ui.common.BaseFragment;
import com.heban.app.ui.common.Snackbars;
import com.heban.app.util.PasswordStrength;

/** A05 注册：验证手机号并设置登录密码（第 1 步，共 2 步）。与 A06 共享 {@link RegisterViewModel}。 */
public class RegisterFragment extends BaseFragment {
    private FragmentRegisterBinding binding;
    private RegisterViewModel vm;

    /** register_graph 返回栈条目作用域内的共享 ViewModel（A05 / A06 共用） */
    static RegisterViewModel sharedViewModel(Fragment f) {
        NavBackStackEntry entry = NavHostFragment.findNavController(f).getBackStackEntry(R.id.register_graph);
        return new ViewModelProvider(entry, HeBanApp.factory(f.requireContext())).get(RegisterViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = sharedViewModel(this);
        binding.setVm(vm);
        binding.setOpenAgreement(v -> nav().navigate(R.id.aboutFragment));
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.toolbar.setNavigationOnClickListener(v -> nav().navigateUp());
        binding.etPhone.addTextChangedListener(new RegisterPhoneFormatter());
        setUpPhonePrefix();
        setUpCodeButton();
        binding.btnToLogin.setOnClickListener(v -> backToLogin());

        vm.strength.observe(getViewLifecycleOwner(), this::renderStrength);
        vm.passwordsMatch.observe(getViewLifecycleOwner(),
                match -> binding.tilConfirm.setEndIconVisible(Boolean.TRUE.equals(match)));
        // 错误提示只在不一致时占位，恢复一致后收起，避免框下方留白
        vm.confirmMismatch.observe(getViewLifecycleOwner(), mismatch -> {
            boolean on = Boolean.TRUE.equals(mismatch);
            binding.tilConfirm.setError(on ? getString(R.string.register_password_mismatch) : null);
            binding.tilConfirm.setErrorEnabled(on);
        });
        vm.loading.observe(getViewLifecycleOwner(),
                loading -> showButtonLoading(binding.btnNext, Boolean.TRUE.equals(loading)));

        vm.toast.observe(getViewLifecycleOwner(), msg -> Snackbars.show(binding.getRoot(), msg));
        vm.toastRes.observe(getViewLifecycleOwner(), res -> Snackbars.show(binding.getRoot(), getString(res)));
        vm.fakeSmsSent.observe(getViewLifecycleOwner(),
                code -> Snackbars.show(binding.getRoot(), getString(R.string.fake_sms_hint, code)));
        vm.toProfileSetup.observe(getViewLifecycleOwner(), go -> {
            NavDestination cur = nav().getCurrentDestination();
            if (cur != null && cur.getId() == R.id.registerFragment) nav().navigate(R.id.profileSetupFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** 「+86」后面加一条竖线，与原型一致 */
    private void setUpPhonePrefix() {
        TextView prefix = binding.tilPhone.getPrefixTextView();
        Drawable divider = ContextCompat.getDrawable(requireContext(), R.drawable.register_field_divider);
        // 竖线两侧各 8dp：左侧用 drawablePadding，右侧用 InsetDrawable（前缀的 padding 会被 TextInputLayout 重置）
        prefix.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, fieldDivider(divider, dp(8)), null);
        prefix.setCompoundDrawablePadding(dp(8));
        prefix.setIncludeFontPadding(false);
        // 前缀与输入文字的基线对齐（两者高度不同，TextInputLayout 只按顶部对齐）
        View input = binding.etPhone;
        View.OnLayoutChangeListener align = (v, l, t, r, b, ol, ot, or, ob) -> alignBaseline(prefix, input);
        prefix.addOnLayoutChangeListener(align);
        input.addOnLayoutChangeListener(align);
    }

    /** 验证码框右侧的文字按钮：竖线 + 「获取验证码 / 42s 后重发 / 重新获取」 */
    private void setUpCodeButton() {
        TextView btn = binding.tilCode.getSuffixTextView();
        Drawable divider = ContextCompat.getDrawable(requireContext(), R.drawable.register_field_divider);
        btn.setCompoundDrawablesRelativeWithIntrinsicBounds(fieldDivider(divider, 0), null, null, null);
        btn.setCompoundDrawablePadding(dp(20));
        btn.setMinHeight(dp(40));
        btn.setGravity(Gravity.CENTER_VERTICAL);
        btn.setIncludeFontPadding(false);
        btn.setPaddingRelative(dp(8), 0, 0, 0);
        btn.setBackgroundResource(resolveAttrRes(android.R.attr.selectableItemBackground));
        btn.setOnClickListener(v -> vm.sendCode());
        vm.countdown.canSend.observe(getViewLifecycleOwner(), can -> {
            boolean enabled = Boolean.TRUE.equals(can);
            btn.setEnabled(enabled);
            btn.setClickable(enabled);
            binding.tilCode.setSuffixTextColor(ColorStateList.valueOf(
                    color(enabled ? androidx.appcompat.R.attr.colorPrimary
                            : com.google.android.material.R.attr.colorOutline)));
        });
    }

    /** prefix 位于 StartCompoundLayout 内，与输入框同在 TextInputLayout 的输入区 FrameLayout 中 */
    private static void alignBaseline(TextView prefix, View input) {
        if (prefix.getBaseline() < 0 || input.getBaseline() < 0) return;
        View parent = (View) prefix.getParent();
        float prefixBase = parent.getTop() + prefix.getTop() + prefix.getBaseline();
        float inputBase = input.getTop() + input.getBaseline();
        prefix.setTranslationY(inputBase - prefixBase);
    }

    /**
     * 输入框内的竖线。上下用负的 inset 让它不参与高度计算（仍画出 24dp），
     * 否则前缀 / 后缀文字连同 TextInputLayout 设置的上下 padding 会把输入框撑高到 58dp。
     */
    private Drawable fieldDivider(Drawable divider, int endGap) {
        int overhang = dp(4);
        return new InsetDrawable(divider, 0, -overhang, endGap, -overhang);
    }

    /** 强度条三段、「强度：中」颜色与三条规则的图标颜色 */
    private void renderStrength(PasswordStrength.Result r) {
        int idle = color(com.google.android.material.R.attr.colorSurfaceContainerHighest);
        int levelColor;
        int filled;
        switch (r.level) {
            case WEAK:
                levelColor = color(androidx.appcompat.R.attr.colorError);
                filled = 1;
                break;
            case MEDIUM:
                levelColor = color(R.attr.colorWarning);
                filled = 2;
                break;
            case STRONG:
                levelColor = color(androidx.appcompat.R.attr.colorPrimary);
                filled = 3;
                break;
            default:
                levelColor = color(com.google.android.material.R.attr.colorOnSurfaceVariant);
                filled = 0;
                break;
        }
        View[] segments = {binding.strengthSeg1, binding.strengthSeg2, binding.strengthSeg3};
        for (int i = 0; i < segments.length; i++) {
            segments[i].setBackgroundTintList(ColorStateList.valueOf(i < filled ? levelColor : idle));
        }
        binding.tvStrength.setTextColor(levelColor);
        renderRule(binding.tvRuleLength, r.lengthOk);
        renderRule(binding.tvRuleLetter, r.hasLetter);
        renderRule(binding.tvRuleDigit, r.hasDigit);
    }

    /** 满足：实心 check_circle + Primary；未满足：radio_button_unchecked + Outline */
    private void renderRule(TextView tv, boolean ok) {
        int c = color(ok ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOutline);
        @DrawableRes int icon = ok ? R.drawable.ic_check_circle_fill : R.drawable.ic_radio_button_unchecked;
        Drawable d = ContextCompat.getDrawable(requireContext(), icon);
        if (d != null) d.setBounds(0, 0, dp(16), dp(16));
        tv.setCompoundDrawablesRelative(d, null, null, null);
        TextViewCompat.setCompoundDrawableTintList(tv, ColorStateList.valueOf(c));
        tv.setTextColor(c);
    }

    /** 已有账号？去登录：登录页在返回栈中就退回去，否则（从引导页进入）打开登录页并关闭注册流程 */
    private void backToLogin() {
        if (!nav().popBackStack(R.id.loginFragment, false)) {
            nav().navigate(R.id.loginFragment, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.register_graph, true)
                    .build());
        }
    }

    /** 提交中：按钮内显示小号环形进度（UI 设计文档：按钮内的操作用 CircularProgressIndicator） */
    static void showButtonLoading(MaterialButton button, boolean loading) {
        if (!loading) {
            // 先停止无限循环的进度动画，再移除图标
            if (button.getIcon() instanceof IndeterminateDrawable) button.getIcon().setVisible(false, false);
            button.setIcon(null);
            return;
        }
        if (button.getIcon() instanceof IndeterminateDrawable) return;
        CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(button.getContext(), null, 0,
                com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
        spec.indicatorColors = new int[]{button.getCurrentTextColor()};
        button.setIcon(IndeterminateDrawable.createCircularDrawable(button.getContext(), spec));
    }

    private int color(@AttrRes int attr) {
        return MaterialColors.getColor(binding.getRoot(), attr);
    }

    private int resolveAttrRes(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.resourceId;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
