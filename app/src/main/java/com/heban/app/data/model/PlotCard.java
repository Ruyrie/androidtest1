package com.heban.app.data.model;

import com.heban.app.data.local.entity.PlotEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** C05 地块卡片。 */
public final class PlotCard {
    public PlotEntity plot;
    /** 地块中的在种作物 */
    public List<CropCard> crops = new ArrayList<>();
    /** 本季投入（该地块作物的支出合计，分） */
    public long seasonCostCent;
    /** 最近一次灌溉记录时间，null 表示没有 */
    public Long lastIrrigationAt;
    /** 棚内湿度 ≥ 85% */
    public boolean humidityHigh;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlotCard)) return false;
        PlotCard p = (PlotCard) o;
        return seasonCostCent == p.seasonCostCent && humidityHigh == p.humidityHigh
                && Objects.equals(lastIrrigationAt, p.lastIrrigationAt) && Objects.equals(crops, p.crops)
                && plot != null && p.plot != null && plot.id == p.plot.id
                && Objects.equals(plot.name, p.plot.name) && plot.areaMu == p.plot.areaMu
                && Objects.equals(plot.indoorTemp, p.plot.indoorTemp)
                && Objects.equals(plot.indoorHumidity, p.plot.indoorHumidity)
                && Objects.equals(plot.structure, p.plot.structure)
                && Objects.equals(plot.irrigation, p.plot.irrigation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plot == null ? 0 : plot.id, seasonCostCent);
    }
}
