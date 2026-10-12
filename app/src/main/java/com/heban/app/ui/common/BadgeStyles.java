package com.heban.app.ui.common;

import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;

/**
 * 身份徽标配色（原型）：种植能手 = Warning 容器色，农技专家 / 机构 = Tertiary 容器色，官方 = Primary 容器色，
 * 等级「Lv.N」= surfaceContainerHighest。徽标高 20–22dp、圆角 6dp（UI 设计文档）。
 */
public final class BadgeStyles {
    private BadgeStyles() {
    }

    /** 显示徽标；badge 为空时隐藏 */
    public static void apply(TextView view, @Nullable String badge) {
        if (badge == null || badge.trim().isEmpty()) {
            view.setVisibility(android.view.View.GONE);
            return;
        }
        view.setVisibility(android.view.View.VISIBLE);
        view.setText(badge);
        int bgAttr;
        int fgAttr;
        if (badge.contains("种植能手")) {
            bgAttr = R.attr.colorWarningContainer;
            fgAttr = R.attr.colorOnWarningContainer;
        } else if (badge.contains("专家") || badge.contains("机构")) {
            bgAttr = com.google.android.material.R.attr.colorTertiaryContainer;
            fgAttr = com.google.android.material.R.attr.colorOnTertiaryContainer;
        } else if (badge.contains("官方")) {
            bgAttr = com.google.android.material.R.attr.colorPrimaryContainer;
            fgAttr = com.google.android.material.R.attr.colorOnPrimaryContainer;
        } else {
            bgAttr = com.google.android.material.R.attr.colorSurfaceContainerHighest;
            fgAttr = com.google.android.material.R.attr.colorOnSurfaceVariant;
        }
        view.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(view, bgAttr)));
        view.setTextColor(MaterialColors.getColor(view, fgAttr));
    }

    /** 农友徽标文字：有身份用身份，否则「Lv.N」 */
    public static String label(@Nullable String badge, int level) {
        if (badge != null && !badge.trim().isEmpty()) return badge;
        return level > 0 ? "Lv." + level : "";
    }
}
