package com.heban.app.data.model;

/** C05 顶部统计：地块数 / 总面积（亩）/ 利用率。 */
public final class PlotStats {
    public int count;
    public double totalAreaMu;
    /** 利用率 0–100 = 作物面积合计 ÷ 地块面积合计 */
    public int utilization;
}
