package com.heban.app.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 分步页面顶部的步骤条（A07 / A08 / A09 / M07）：圆形序号 + 步骤名，步骤之间用细线连接。
 * <ul>
 *   <li>当前步骤：Primary 实心圆、On Primary 序号，步骤名 Title Small（On Surface）</li>
 *   <li>已完成：Primary Container 圆内显示对勾，步骤名 Body Medium（On Surface Variant）</li>
 *   <li>未开始：Outline 描边圆，序号和步骤名为 On Surface Variant</li>
 *   <li>连接线：前一步已完成时为 Primary，否则为 Outline Variant</li>
 * </ul>
 * 用法：XML 中 {@code android:entries="@array/..."} 提供步骤名，
 * DataBinding 中 {@code app:currentStep="@{vm.step}"}（从 1 开始），或调用 {@link #setSteps} / {@link #setCurrentStep}。
 */
public class StepIndicatorView extends LinearLayout {
    private static final int CIRCLE_DP = 28;
    private static final int CHECK_DP = 18;
    private static final int GAP_DP = 8;

    private final List<CharSequence> labels = new ArrayList<>();
    private int currentStep = 1;

    public StepIndicatorView(@NonNull Context context) {
        this(context, null);
    }

    public StepIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StepIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.entries});
            CharSequence[] entries = a.getTextArray(0);
            a.recycle();
            if (entries != null) labels.addAll(Arrays.asList(entries));
        }
        rebuild();
    }

    /** 设置步骤名（按顺序） */
    public void setSteps(@Nullable CharSequence... steps) {
        labels.clear();
        if (steps != null) labels.addAll(Arrays.asList(steps));
        rebuild();
    }

    /** DataBinding：{@code app:steps="@{@stringArray/xxx}"} */
    public void setSteps(@Nullable String[] steps) {
        setSteps((CharSequence[]) steps);
    }

    /** 当前步骤，从 1 开始；大于步骤数表示全部完成 */
    public void setCurrentStep(int step) {
        if (step == currentStep && getChildCount() > 0) return;
        currentStep = step;
        rebuild();
    }

    public int getCurrentStep() {
        return currentStep;
    }

    private void rebuild() {
        removeAllViews();
        int total = labels.size();
        for (int i = 0; i < total; i++) {
            int number = i + 1;
            if (i > 0) addView(line(number - 1 < currentStep));
            addView(item(number, total, labels.get(i)));
        }
    }

    private View item(int number, int total, CharSequence label) {
        boolean done = number < currentStep;
        boolean current = number == currentStep;

        LinearLayout item = new LinearLayout(getContext());
        item.setOrientation(HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        String state = getContext().getString(done ? R.string.step_indicator_done
                : current ? R.string.step_indicator_current : R.string.step_indicator_upcoming);
        item.setContentDescription(getContext().getString(
                R.string.step_indicator_cd, number, total, label, state));
        item.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        FrameLayout circle = new FrameLayout(getContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        if (done) {
            bg.setColor(color(com.google.android.material.R.attr.colorPrimaryContainer));
        } else if (current) {
            bg.setColor(color(androidx.appcompat.R.attr.colorPrimary));
        } else {
            bg.setColor(Color.TRANSPARENT);
            bg.setStroke(dp(1), color(com.google.android.material.R.attr.colorOutline));
        }
        circle.setBackground(bg);
        circle.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        if (done) {
            ImageView check = new ImageView(getContext());
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(ColorStateList.valueOf(
                    color(com.google.android.material.R.attr.colorOnPrimaryContainer)));
            circle.addView(check, new FrameLayout.LayoutParams(dp(CHECK_DP), dp(CHECK_DP), Gravity.CENTER));
        } else {
            TextView num = new TextView(getContext());
            num.setText(String.valueOf(number));
            num.setTextAppearance(resolveResource(com.google.android.material.R.attr.textAppearanceLabelLarge));
            num.setTextColor(color(current ? com.google.android.material.R.attr.colorOnPrimary
                    : com.google.android.material.R.attr.colorOnSurfaceVariant));
            num.setIncludeFontPadding(false);
            num.setGravity(Gravity.CENTER);
            circle.addView(num, new FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        }
        item.addView(circle, new LayoutParams(dp(CIRCLE_DP), dp(CIRCLE_DP)));

        TextView text = new TextView(getContext());
        text.setText(label);
        text.setMaxLines(1);
        text.setFallbackLineSpacing(false);   // 中文行高与原型一致（20dp）
        text.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        text.setTextAppearance(resolveResource(current
                ? com.google.android.material.R.attr.textAppearanceTitleSmall
                : com.google.android.material.R.attr.textAppearanceBodyMedium));
        text.setTextColor(color(current ? com.google.android.material.R.attr.colorOnSurface
                : com.google.android.material.R.attr.colorOnSurfaceVariant));
        LayoutParams tlp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        tlp.setMarginStart(dp(GAP_DP));
        item.addView(text, tlp);

        item.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        return item;
    }

    private View line(boolean previousDone) {
        View v = new View(getContext());
        v.setBackgroundColor(color(previousDone ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOutlineVariant));
        v.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        LayoutParams lp = new LayoutParams(0, dp(1), 1f);
        lp.setMarginStart(dp(GAP_DP));
        lp.setMarginEnd(dp(GAP_DP));
        v.setLayoutParams(lp);
        return v;
    }

    private int color(@AttrRes int attr) {
        return MaterialColors.getColor(this, attr);
    }

    private int resolveResource(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.resourceId;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }
}
