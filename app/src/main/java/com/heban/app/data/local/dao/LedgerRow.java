package com.heban.app.data.local.dao;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.heban.app.data.local.entity.LedgerEntryEntity;

/** 记账条目 + 关联作物名称 + 作物所在地块名称（D10 流水列表）。 */
public class LedgerRow {
    @Embedded
    public LedgerEntryEntity entry;

    @ColumnInfo(name = "crop_name")
    public String cropName;

    @ColumnInfo(name = "plot_name")
    public String plotName;
}
