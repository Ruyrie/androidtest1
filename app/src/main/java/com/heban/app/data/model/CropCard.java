package com.heban.app.data.model;

import androidx.annotation.DrawableRes;

import java.util.Objects;

/** 作物卡片（H01「我的作物」、C01 列表）。 */
public final class CropCard {
    public long id;
    public String name;
    public String variety;
    @DrawableRes public int icon;
    public String coverPath;
    /** 当前生长阶段，如「结果期」 */
    public String stage;
    public String plotName;
    public double areaMu;
    public String plantMode;
    /** 已种天数（定植当天为第 1 天） */
    public int daysPlanted;
    public int expectedDays;
    /** 生长进度 0–100 */
    public int progress;
    /** 需关注说明，null 表示无 */
    public String attentionNote;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CropCard)) return false;
        CropCard c = (CropCard) o;
        return id == c.id && icon == c.icon && Double.compare(c.areaMu, areaMu) == 0
                && daysPlanted == c.daysPlanted && expectedDays == c.expectedDays && progress == c.progress
                && Objects.equals(name, c.name) && Objects.equals(variety, c.variety)
                && Objects.equals(coverPath, c.coverPath) && Objects.equals(stage, c.stage)
                && Objects.equals(plotName, c.plotName) && Objects.equals(plantMode, c.plantMode)
                && Objects.equals(attentionNote, c.attentionNote);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, stage, daysPlanted, attentionNote);
    }
}
