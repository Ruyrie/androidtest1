package com.heban.app.ui.account;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.widget.ImageViewCompat;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.databinding.FragmentOnboardingBinding;
import com.heban.app.ui.common.BaseFragment;

/**
 * A02 引导页（原型 Onboarding.dc.html）：同一个 Fragment 内切换 3 屏内容（当前屏保存在 Fragment 状态中，旋转屏幕不丢失）。
 * 「下一步」/ 左右滑动切换；最后一屏「开始使用」进入 A05 注册；「跳过」「已有账号？登录」进入 A03。
 * 离开引导页时写入 onboarding.done，下次启动直接进入登录页。
 */
public class OnboardingFragment extends BaseFragment {
    private static final String STATE_STEP = "onboarding.step";

    private static final class Step {
        @DrawableRes final int icon;
        @AttrRes final int heroBg;
        @AttrRes final int heroFg;
        @StringRes final int title;
        @StringRes final int body;
        @DrawableRes final int chipIconA;
        @StringRes final int chipA;
        @DrawableRes final int chipIconB;
        @StringRes final int chipB;

        Step(int icon, int heroBg, int heroFg, int title, int body,
             int chipIconA, int chipA, int chipIconB, int chipB) {
            this.icon = icon;
            this.heroBg = heroBg;
            this.heroFg = heroFg;
            this.title = title;
            this.body = body;
            this.chipIconA = chipIconA;
            this.chipA = chipA;
            this.chipIconB = chipIconB;
            this.chipB = chipB;
        }
    }

    private static final Step[] STEPS = {
            new Step(R.drawable.ic_potted_plant_fill,
                    com.google.android.material.R.attr.colorPrimaryContainer,
                    com.google.android.material.R.attr.colorOnPrimaryContainer,
                    R.string.onboarding_title_1, R.string.onboarding_body_1,
                    R.drawable.ic_nutrition, R.string.onboarding_chip_a_1,
                    R.drawable.ic_notifications, R.string.onboarding_chip_b_1),
            new Step(R.drawable.ic_auto_awesome_fill,
                    com.google.android.material.R.attr.colorTertiaryContainer,
                    com.google.android.material.R.attr.colorOnTertiaryContainer,
                    R.string.onboarding_title_2, R.string.onboarding_body_2,
                    R.drawable.ic_photo_camera, R.string.onboarding_chip_a_2,
                    R.drawable.ic_spa, R.string.onboarding_chip_b_2),
            new Step(R.drawable.ic_forum_fill,
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    com.google.android.material.R.attr.colorOnSecondaryContainer,
                    R.string.onboarding_title_3, R.string.onboarding_body_3,
                    R.drawable.ic_favorite, R.string.onboarding_chip_a_3,
                    R.drawable.ic_question_answer, R.string.onboarding_chip_b_3),
    };
    private static final int LAST = STEPS.length - 1;

    private FragmentOnboardingBinding binding;
    private int step;
    private final OnBackPressedCallback backToPrevious = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            showStep(step - 1);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) step = savedInstanceState.getInt(STATE_STEP, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnSkip.setOnClickListener(v -> leaveTo(R.id.loginFragment));
        binding.linkLogin.setOnClickListener(v -> leaveTo(R.id.loginFragment));
        binding.btnNext.setOnClickListener(v -> {
            if (step < LAST) showStep(step + 1);
            else leaveTo(R.id.register_graph);
        });
        setUpSwipe();
        // 第 2、3 屏按返回键回到上一屏；第 1 屏交给 MainActivity（再按一次退出）
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backToPrevious);
        render();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, step);
    }

    /** 左右滑动切换（与「下一步」效果相同，最后一屏不会自动进入注册） */
    @SuppressLint("ClickableViewAccessibility")
    private void setUpSwipe() {
        final float minDistance = 48 * getResources().getDisplayMetrics().density;
        GestureDetector detector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return false;
            }

            @Override
            public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float vx, float vy) {
                if (e1 == null) return false;
                float dx = e2.getX() - e1.getX();
                if (Math.abs(dx) < minDistance || Math.abs(dx) < Math.abs(e2.getY() - e1.getY())) return false;
                showStep(dx < 0 ? step + 1 : step - 1);
                return true;
            }
        });
        binding.scroll.setOnTouchListener((v, event) -> {
            detector.onTouchEvent(event);
            return false;
        });
    }

    private void showStep(int index) {
        int target = Math.max(0, Math.min(LAST, index));
        if (target == step) return;
        step = target;
        render();
    }

    private void render() {
        if (binding == null) return;
        Step s = STEPS[step];
        View root = binding.getRoot();
        binding.heroCircle.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(root, s.heroBg)));
        binding.heroIcon.setImageResource(s.icon);
        ImageViewCompat.setImageTintList(binding.heroIcon,
                ColorStateList.valueOf(MaterialColors.getColor(root, s.heroFg)));
        binding.chipAIcon.setImageResource(s.chipIconA);
        binding.chipAText.setText(s.chipA);
        binding.chipBIcon.setImageResource(s.chipIconB);
        binding.chipBText.setText(s.chipB);
        binding.tvTitle.setText(s.title);
        binding.tvBody.setText(s.body);
        binding.btnNext.setText(step == LAST ? R.string.onboarding_start : R.string.onboarding_next);

        int active = MaterialColors.getColor(root, androidx.appcompat.R.attr.colorPrimary);
        int inactive = MaterialColors.getColor(root, com.google.android.material.R.attr.colorOutlineVariant);
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < binding.dots.getChildCount(); i++) {
            View dot = binding.dots.getChildAt(i);
            ViewGroup.LayoutParams lp = dot.getLayoutParams();
            lp.width = Math.round((i == step ? 24 : 8) * density);
            dot.setLayoutParams(lp);
            dot.setBackgroundTintList(ColorStateList.valueOf(i == step ? active : inactive));
        }
        binding.dots.setContentDescription(getString(R.string.onboarding_progress, step + 1, STEPS.length));
        backToPrevious.setEnabled(step > 0);
    }

    /**
     * 离开引导页：写入 onboarding.done（不随页面销毁而取消）后跳转。
     * 去登录页时把引导页移出返回栈（登录页成为账号流程的第一页）；去注册时保留，便于返回。
     */
    private void leaveTo(@IdRes int dest) {
        NavDestination current = nav().getCurrentDestination();
        if (current == null || current.getId() != R.id.onboardingFragment) return;
        //noinspection ResultOfMethodCallIgnored
        container().userRepository.completeOnboarding().subscribe(() -> { }, e -> { });
        if (dest == R.id.loginFragment) {
            nav().navigate(dest, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.onboardingFragment, true)
                    .build());
        } else {
            nav().navigate(dest);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
