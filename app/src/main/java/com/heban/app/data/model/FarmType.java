package com.heban.app.data.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.heban.app.R;

/** 农事类型（C04 记录类型、H04 农事类型共用），图标与原型一致。 */
public enum FarmType {
    FERTILIZE("施肥", R.drawable.ic_compost),
    SPRAY("用药", R.drawable.ic_sanitizer),
    IRRIGATE("灌溉", R.drawable.ic_water_drop),
    PRUNE("整枝", R.drawable.ic_content_cut),
    HARVEST("采收", R.drawable.ic_shopping_basket),
    OTHER("其他", R.drawable.ic_more_horiz);

    public final String label;
    @DrawableRes public final int icon;

    FarmType(String label, @DrawableRes int icon) {
        this.label = label;
        this.icon = icon;
    }

    @NonNull
    public static FarmType of(String code) {
        try {
            return FarmType.valueOf(code);
        } catch (Exception e) {
            return OTHER;
        }
    }
}
