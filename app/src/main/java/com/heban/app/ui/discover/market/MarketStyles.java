package com.heban.app.ui.discover.market;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;

/**
 * 集市的颜色规则（全部取主题属性，深色模式自动适配）：
 * 列表角标按标签文字取色（原型 Market.dc.html），头像按用户 / 农友 id 固定取色（UI 设计文档第 7 章）。
 */
final class MarketStyles {
    /** 头像容器色（与 colors.xml avatar_1…5 顺序一致）：背景属性、文字属性 */
    private static final int[][] AVATARS = {
            {androidx.appcompat.R.attr.colorPrimary, com.google.android.material.R.attr.colorOnPrimary},
            {com.google.android.material.R.attr.colorTertiaryContainer,
                    com.google.android.material.R.attr.colorOnTertiaryContainer},
            {R.attr.colorWarningContainer, R.attr.colorOnWarningContainer},
            {com.google.android.material.R.attr.colorSecondaryContainer,
                    com.google.android.material.R.attr.colorOnSecondaryContainer},
            {com.google.android.material.R.attr.colorPrimaryContainer,
                    com.google.android.material.R.attr.colorOnPrimaryContainer},
    };

    private MarketStyles() {
    }

    /** 列表角标：置顶 Warning Container；产地直供 Primary Container；可配送 Tertiary Container；种苗 Secondary Container；其他（二手等）Surface Container Highest */
    static void applyTag(@NonNull TextView view, @Nullable String tag, boolean pinned) {
        int bg;
        int fg;
        if (pinned) {
            bg = R.attr.colorWarningContainer;
            fg = R.attr.colorOnWarningContainer;
        } else if ("产地直供".equals(tag)) {
            bg = com.google.android.material.R.attr.colorPrimaryContainer;
            fg = com.google.android.material.R.attr.colorOnPrimaryContainer;
        } else if ("可配送".equals(tag)) {
            bg = com.google.android.material.R.attr.colorTertiaryContainer;
            fg = com.google.android.material.R.attr.colorOnTertiaryContainer;
        } else if ("种苗".equals(tag)) {
            bg = com.google.android.material.R.attr.colorSecondaryContainer;
            fg = com.google.android.material.R.attr.colorOnSecondaryContainer;
        } else {
            bg = com.google.android.material.R.attr.colorSurfaceContainerHighest;
            fg = com.google.android.material.R.attr.colorOnSurfaceVariant;
        }
        tint(view, bg, fg);
    }

    /** 头像：id 为农友 id（模拟商品）或账号 id（自己发布的） */
    static void applyAvatar(@NonNull TextView view, long id) {
        int[] pair = AVATARS[(int) Math.floorMod(id - 1, (long) AVATARS.length)];
        tint(view, pair[0], pair[1]);
    }

    static long avatarSeed(@NonNull com.heban.app.data.local.entity.ProductEntity p) {
        return p.mine ? p.userId : p.sellerId;
    }

    private static void tint(TextView view, @AttrRes int bg, @AttrRes int fg) {
        view.setBackgroundTintList(ColorStateList.valueOf(color(view, bg)));
        view.setTextColor(color(view, fg));
    }

    static int color(@NonNull View view, @AttrRes int attr) {
        return MaterialColors.getColor(view, attr);
    }

    static int color(@NonNull Context context, @AttrRes int attr) {
        return MaterialColors.getColor(context, attr, 0);
    }
}
