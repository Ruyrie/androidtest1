package com.heban.app.data.local.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/** 农事任务的时间与类型（H03 日历圆点）。 */
public class TaskDayType {
    @ColumnInfo(name = "due_at")
    public long dueAt;

    /** FarmType.code */
    @NonNull
    public String type = "OTHER";
}
