package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 消息通知（S08 四个入口：评论回复、赞和收藏、新增关注、系统通知）。按账号隔离。 */
@Entity(tableName = "notification", indices = @Index("user_id"))
public class NotificationEntity {
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_FOLLOW = "FOLLOW";
    public static final String TYPE_SYSTEM = "SYSTEM";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    public String type = TYPE_SYSTEM;

    @NonNull
    public String title = "";

    public String content;

    /** 点击目标：post / question / farmer / order …，可为空 */
    @ColumnInfo(name = "target_type")
    public String targetType;

    @ColumnInfo(name = "target_id")
    public long targetId;

    public boolean read;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
