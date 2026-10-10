package com.heban.app.data.local.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/** 农事任务 + 关联作物名称 + 作物所在地块名称（H01 / H03 列表）。 */
public class TaskRow {
    public long id;

    @NonNull
    public String title = "";

    /** FarmType.code */
    @NonNull
    public String type = "OTHER";

    @ColumnInfo(name = "crop_id")
    public Long cropId;

    @ColumnInfo(name = "crop_name")
    public String cropName;

    @ColumnInfo(name = "plot_name")
    public String plotName;

    @ColumnInfo(name = "due_at")
    public long dueAt;

    public boolean done;

    public boolean attention;
}
