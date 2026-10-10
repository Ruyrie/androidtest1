package com.heban.app.data.model;

/** 积分概况（M01、H05、P01）。 */
public final class PointsSummary {
    public int balance;
    public int growth;
    /** 1–6 */
    public int level;
    /** Lv.4 起为「种植能手」，之前为空 */
    public String levelTitle;
    /** 距下一级还差的成长值；已满级为 0 */
    public int nextLevelGap;
    /** 本月获得 / 本月消耗（消耗为正数） */
    public int monthGain;
    public int monthSpend;
}
