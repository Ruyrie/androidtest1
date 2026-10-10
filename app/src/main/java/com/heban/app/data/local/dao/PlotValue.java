package com.heban.app.data.local.dao;

import androidx.room.ColumnInfo;

/** 按地块汇总的数值（本季投入合计、最近灌溉时间等），C05 使用。 */
public class PlotValue {
    @ColumnInfo(name = "plot_id")
    public long plotId;

    public long value;
}
