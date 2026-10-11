package com.heban.app.ui.crop;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.heban.app.R;
import com.heban.app.data.model.FarmType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * C04 表单字段随记录类型变化（技术准备文档 12.3）：
 * 施肥 → 肥料名称 / 用量 + 单位；用药 → 药剂名称 / 用量（含稀释倍数）；灌溉 → 灌溉方式 / 水量；
 * 采收 → 采收量；整枝 → 作业内容；其他 → 内容。费用（元）所有类型都有。
 */
public final class AddRecordFields {
    /** 物料 / 内容输入框标题，0 表示不显示 */
    @StringRes public final int materialLabel;
    /** 物料输入框右侧搜索图标弹出的常用名称 */
    @ArrayRes public final int suggestions;
    /** 用量输入框标题，0 表示不显示（同时不显示单位） */
    @StringRes public final int amountLabel;
    public final List<String> units;
    /** 必填的主字段：采收为采收量，其余为物料 / 内容 */
    public final boolean amountRequired;
    /** 同步记账时的支出类别（与 D11 记账类别一致） */
    @NonNull public final String ledgerCategory;

    private AddRecordFields(@StringRes int materialLabel, @ArrayRes int suggestions, @StringRes int amountLabel,
                            List<String> units, boolean amountRequired, @NonNull String ledgerCategory) {
        this.materialLabel = materialLabel;
        this.suggestions = suggestions;
        this.amountLabel = amountLabel;
        this.units = units;
        this.amountRequired = amountRequired;
        this.ledgerCategory = ledgerCategory;
    }

    public boolean hasMaterial() {
        return materialLabel != 0;
    }

    public boolean hasAmount() {
        return amountLabel != 0;
    }

    public boolean materialRequired() {
        return hasMaterial() && !amountRequired;
    }

    @Nullable
    public String defaultUnit() {
        return units.isEmpty() ? null : units.get(0);
    }

    /** 日期后面显示的时段：12 点前为上午，18 点前为下午，之后为晚上 */
    @StringRes
    public static int dayPartRes(int hour) {
        if (hour < 12) return R.string.add_record_morning;
        if (hour < 18) return R.string.add_record_afternoon;
        return R.string.add_record_evening;
    }

    @NonNull
    public static AddRecordFields of(@NonNull FarmType type) {
        switch (type) {
            case FERTILIZE:
                return new AddRecordFields(R.string.add_record_material_fertilize, R.array.add_record_suggest_fertilize,
                        R.string.add_record_amount, Arrays.asList("kg", "斤", "g", "袋", "L", "mL"), false, "化肥");
            case SPRAY:
                return new AddRecordFields(R.string.add_record_material_spray, R.array.add_record_suggest_spray,
                        R.string.add_record_amount, Arrays.asList("mL", "g", "L", "kg", "倍"), false, "农药");
            case IRRIGATE:
                return new AddRecordFields(R.string.add_record_material_irrigate, R.array.add_record_suggest_irrigate,
                        R.string.add_record_amount_water, Arrays.asList("吨", "m³", "L"), false, "水电灌溉");
            case PRUNE:
                return new AddRecordFields(R.string.add_record_material_prune, R.array.add_record_suggest_prune,
                        0, Collections.emptyList(), false, "人工");
            case HARVEST:
                return new AddRecordFields(0, 0,
                        R.string.add_record_amount_harvest, Arrays.asList("kg", "斤", "箱"), true, "人工");
            case OTHER:
            default:
                return new AddRecordFields(R.string.add_record_material_other, R.array.add_record_suggest_other,
                        0, Collections.emptyList(), false, "其他");
        }
    }
}
