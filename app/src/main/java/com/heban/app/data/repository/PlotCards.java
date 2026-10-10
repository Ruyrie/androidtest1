package com.heban.app.data.repository;

import com.heban.app.data.local.dao.PlotValue;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.model.PlotStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** C05 地块卡片与统计（纯 Java）。 */
public final class PlotCards {
    /** 棚内湿度 ≥ 85% 显示「湿度偏高」 */
    public static final int HUMIDITY_HIGH = 85;

    private PlotCards() {
    }

    public static boolean humidityHigh(PlotEntity plot) {
        return plot.indoorHumidity != null && plot.indoorHumidity >= HUMIDITY_HIGH;
    }

    /**
     * @param crops    当前账号的作物（顺序即卡片中作物的顺序）
     * @param cards    作物 id → 作物卡片
     * @param costs    地块 id → 支出合计（分）
     * @param irrigate 地块 id → 最近灌溉时间
     */
    public static List<PlotCard> cards(List<PlotEntity> plots, List<CropEntity> crops, Map<Long, CropCard> cards,
                                       List<PlotValue> costs, List<PlotValue> irrigate) {
        Map<Long, Long> cost = toMap(costs);
        Map<Long, Long> lastIrrigation = toMap(irrigate);
        List<PlotCard> out = new ArrayList<>(plots.size());
        for (PlotEntity p : plots) {
            PlotCard card = new PlotCard();
            card.plot = p;
            for (CropEntity c : crops) {
                if (c.plotId != null && c.plotId == p.id) {
                    CropCard cc = cards.get(c.id);
                    if (cc != null) card.crops.add(cc);
                }
            }
            Long sum = cost.get(p.id);
            card.seasonCostCent = sum == null ? 0 : sum;
            card.lastIrrigationAt = lastIrrigation.get(p.id);
            card.humidityHigh = humidityHigh(p);
            out.add(card);
        }
        return out;
    }

    /** 地块数、总面积 = SUM(地块面积)、利用率 = SUM(地块中作物面积) ÷ SUM(地块面积)，四舍五入，最高 100 */
    public static PlotStats stats(List<PlotEntity> plots, List<CropEntity> crops) {
        PlotStats s = new PlotStats();
        s.count = plots.size();
        double total = 0;
        Set<Long> ids = new HashSet<>();
        for (PlotEntity p : plots) {
            total += p.areaMu;
            ids.add(p.id);
        }
        double used = 0;
        for (CropEntity c : crops) {
            if (c.plotId != null && ids.contains(c.plotId)) used += c.areaMu;
        }
        s.totalAreaMu = Math.round(total * 100) / 100.0;
        s.utilization = total > 0 ? (int) Math.min(100, Math.round(used / total * 100)) : 0;
        return s;
    }

    public static boolean sameStats(PlotStats a, PlotStats b) {
        return a.count == b.count && a.utilization == b.utilization
                && Double.compare(a.totalAreaMu, b.totalAreaMu) == 0;
    }

    private static Map<Long, Long> toMap(List<PlotValue> values) {
        Map<Long, Long> m = new HashMap<>();
        for (PlotValue v : values) m.put(v.plotId, v.value);
        return m;
    }
}
