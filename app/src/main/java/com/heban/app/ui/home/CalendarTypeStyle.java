package com.heban.app.ui.home;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.R;
import com.heban.app.data.model.FarmType;

import java.util.Collection;

/**
 * H03 日历中农事类型的配色（原型图例）：
 * 「施肥 / 管理」Primary（施肥、整枝、采收、其他），「灌溉」Tertiary，「用药 / 病虫害」Error。
 */
public enum CalendarTypeStyle {
    MANAGE(androidx.appcompat.R.attr.colorPrimary,
            com.google.android.material.R.attr.colorPrimaryContainer,
            com.google.android.material.R.attr.colorOnPrimaryContainer),
    IRRIGATE(com.google.android.material.R.attr.colorTertiary,
            com.google.android.material.R.attr.colorTertiaryContainer,
            com.google.android.material.R.attr.colorOnTertiaryContainer),
    PEST(androidx.appcompat.R.attr.colorError,
            com.google.android.material.R.attr.colorErrorContainer,
            com.google.android.material.R.attr.colorOnErrorContainer);

    /** 圆点颜色 */
    @AttrRes public final int dot;
    /** 当日农事列表图标底色 / 图标色 */
    @AttrRes public final int container;
    @AttrRes public final int onContainer;

    CalendarTypeStyle(@AttrRes int dot, @AttrRes int container, @AttrRes int onContainer) {
        this.dot = dot;
        this.container = container;
        this.onContainer = onContainer;
    }

    @NonNull
    public static CalendarTypeStyle of(@Nullable FarmType type) {
        if (type == FarmType.IRRIGATE) return IRRIGATE;
        if (type == FarmType.SPRAY) return PEST;
        return MANAGE;
    }

    /** 当日农事列表的图标：用药类按原型显示 pest_control，其余用农事类型图标 */
    @DrawableRes
    public static int listIcon(@Nullable FarmType type) {
        if (type == null) return FarmType.OTHER.icon;
        if (type == FarmType.SPRAY) return R.drawable.ic_pest_control;
        return type.icon;
    }

    /** 某天包含的 FarmType.code 中是否有这一配色 */
    public boolean presentIn(@Nullable Collection<String> codes) {
        if (codes == null) return false;
        for (String code : codes) {
            if (of(FarmType.of(code)) == this) return true;
        }
        return false;
    }
}
