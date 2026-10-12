package com.heban.app.ui.discover.pest;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.heban.app.R;
import com.heban.app.data.local.entity.PestEntity;

import java.util.HashMap;
import java.util.Map;

/** PestEntity.icon（Material Symbol 名称）→ 图标资源；未知名称按类别回退（病害 coronavirus、虫害 bug_report）。 */
public final class PestsIcons {
    private static final Map<String, Integer> ICONS = new HashMap<>();

    static {
        ICONS.put("coronavirus", R.drawable.ic_coronavirus);
        ICONS.put("blur_on", R.drawable.ic_blur_on);
        ICONS.put("bug_report", R.drawable.ic_bug_report);
        ICONS.put("pest_control", R.drawable.ic_pest_control);
        ICONS.put("grass", R.drawable.ic_grass);
        ICONS.put("eco", R.drawable.ic_eco);
        ICONS.put("spa", R.drawable.ic_spa);
        ICONS.put("psychiatry", R.drawable.ic_psychiatry);
        ICONS.put("nutrition", R.drawable.ic_nutrition);
        ICONS.put("water_drop", R.drawable.ic_water_drop);
        ICONS.put("humidity_high", R.drawable.ic_humidity_high);
        ICONS.put("severe_cold", R.drawable.ic_severe_cold);
        ICONS.put("sunny", R.drawable.ic_sunny);
        ICONS.put("warning", R.drawable.ic_warning);
        ICONS.put("local_fire_department", R.drawable.ic_local_fire_department);
    }

    private PestsIcons() {
    }

    @DrawableRes
    public static int forPest(@Nullable PestEntity p) {
        if (p == null) return R.drawable.ic_coronavirus;
        Integer res = p.icon == null ? null : ICONS.get(p.icon.trim());
        if (res != null) return res;
        return PestEntity.KIND_INSECT.equals(p.kind) ? R.drawable.ic_bug_report : R.drawable.ic_coronavirus;
    }
}
