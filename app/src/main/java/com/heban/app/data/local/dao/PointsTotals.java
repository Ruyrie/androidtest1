package com.heban.app.data.local.dao;

import androidx.room.ColumnInfo;

/** 积分概况的原始合计：余额、成长值、本月获得、本月消耗（消耗为正数）。 */
public class PointsTotals {
    public int balance;

    public int growth;

    @ColumnInfo(name = "month_gain")
    public int monthGain;

    @ColumnInfo(name = "month_spend")
    public int monthSpend;
}
