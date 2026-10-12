package com.heban.app.ui.common;

import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.annotation.AttrRes;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;

/**
 * 文字头像配色（UI 设计文档：无头像时用昵称首字 + 容器色，按用户 ID 固定取色，不随机）。
 * 模拟农友 1–8 的颜色与原型一致；当前账号固定用 primary。
 */
public final class AvatarColors {
    private AvatarColors() {
    }

    /** [背景色属性, 文字色属性] */
    private static final int[][] PALETTE = {
            {R.attr.colorTertiaryContainer, R.attr.colorOnTertiaryContainer},       // 0
            {R.attr.colorSecondaryContainer, R.attr.colorOnSecondaryContainer},     // 1
            {R.attr.colorWarningContainer, R.attr.colorOnWarningContainer},         // 2
            {R.attr.colorPrimaryContainer, R.attr.colorOnPrimaryContainer},         // 3
            {R.attr.colorSurfaceContainerHighest, R.attr.colorOnSurfaceVariant},    // 4
    };

    /** 原型中的农友：1 草莓姐姐、2 王农艺师、3 果园阿强、4 菜园小周、5 江宁老陈、6 小林的阳台菜园、7 禾伴官方、8 江苏农技推广 */
    private static final int[] FARMER_SLOT = {-1, 0, 1, 2, 1, 3, 4, -2, 0};

    /** 给文字头像着色：me = 当前账号（primary） */
    public static void apply(TextView avatar, long farmerId, boolean me) {
        int bg;
        int fg;
        if (me || slotOf(farmerId) == -2) {
            bg = MaterialColors.getColor(avatar, R.attr.colorPrimary);
            fg = MaterialColors.getColor(avatar, R.attr.colorOnPrimary);
        } else {
            int[] pair = PALETTE[slotOf(farmerId)];
            bg = MaterialColors.getColor(avatar, pair[0]);
            fg = MaterialColors.getColor(avatar, pair[1]);
        }
        avatar.setBackgroundTintList(ColorStateList.valueOf(bg));
        avatar.setTextColor(fg);
    }

    /** 背景色属性（自定义绘制时使用）；-2 表示 primary */
    @AttrRes
    public static int backgroundAttr(long farmerId, boolean me) {
        int slot = me ? -2 : slotOf(farmerId);
        return slot == -2 ? R.attr.colorPrimary : PALETTE[slot][0];
    }

    static int slotOf(long farmerId) {
        if (farmerId > 0 && farmerId < FARMER_SLOT.length) return FARMER_SLOT[(int) farmerId];
        return (int) (Math.abs(farmerId) % PALETTE.length);
    }
}
