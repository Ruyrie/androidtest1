package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 农事任务（H01 / H03 / H04）。重复任务保存为同一 series_id 的多条记录。 */
@Entity(tableName = "farm_task", indices = {@Index("user_id"), @Index("due_at")})
public class FarmTaskEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "crop_id")
    public Long cropId;

    /** FarmType.code */
    @NonNull
    public String type = "OTHER";

    /** 任务标题，如「给番茄追施钾肥」 */
    @NonNull
    public String title = "";

    @ColumnInfo(name = "due_at")
    public long dueAt;

    /** 提前多少分钟提醒，-1 表示不提醒 */
    @ColumnInfo(name = "remind_minutes")
    public int remindMinutes = 30;

    /** 重复任务的系列标识（第一条的创建时间戳），不重复为 0 */
    @ColumnInfo(name = "series_id")
    public long seriesId;

    public boolean done;

    @ColumnInfo(name = "done_at")
    public long doneAt;

    /** 「需关注」标签 */
    public boolean attention;

    public String note;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
