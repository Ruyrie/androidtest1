package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.model.FarmType;

/** 农事记录同步记账（C04）：记录类型 → 支出类别、备注文字（纯 Java）。 */
public final class LedgerCategories {
    public static final String FERTILIZER = "化肥";
    public static final String PESTICIDE = "农药";
    public static final String IRRIGATION = "水电灌溉";
    public static final String LABOR = "人工";
    public static final String OTHER = "其他";

    private LedgerCategories() {
    }

    /** 施肥 → 化肥，用药 → 农药，灌溉 → 水电灌溉，整枝 → 人工，采收 / 其他 → 其他 */
    public static String forRecordType(@Nullable String farmTypeCode) {
        switch (FarmType.of(farmTypeCode == null ? "" : farmTypeCode)) {
            case FERTILIZE:
                return FERTILIZER;
            case SPRAY:
                return PESTICIDE;
            case IRRIGATE:
                return IRRIGATION;
            case PRUNE:
                return LABOR;
            default:
                return OTHER;
        }
    }

    /** 备注：「施肥 · 硫酸钾」；没有填写物料时只写类型 */
    public static String noteFor(@Nullable String farmTypeCode, @Nullable String material) {
        String label = FarmType.of(farmTypeCode == null ? "" : farmTypeCode).label;
        String m = material == null ? "" : material.trim();
        return m.isEmpty() ? label : label + " · " + m;
    }

    /** 由农事记录生成一笔支出（userId / createdAt 由调用方填写） */
    public static LedgerEntryEntity entryFor(FarmRecordEntity record) {
        LedgerEntryEntity e = new LedgerEntryEntity();
        e.type = LedgerEntryEntity.TYPE_OUT;
        e.category = forRecordType(record.type);
        e.amountCent = record.costCent;
        e.cropId = record.cropId;
        e.date = record.date;
        e.note = noteFor(record.type, record.material);
        e.source = LedgerEntryEntity.SOURCE_RECORD;
        e.recordId = record.id;
        e.userId = record.userId;
        return e;
    }
}
