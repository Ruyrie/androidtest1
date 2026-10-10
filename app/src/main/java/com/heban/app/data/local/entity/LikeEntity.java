package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

/** 当前账号的点赞 / 赞同（动态、评论、回答）。 */
@Entity(tableName = "like_record", primaryKeys = {"user_id", "target_type", "target_id"})
public class LikeEntity {
    public static final String TYPE_POST = "post";
    public static final String TYPE_COMMENT = "comment";

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    @ColumnInfo(name = "target_type")
    public String targetType = TYPE_POST;

    @ColumnInfo(name = "target_id")
    public long targetId;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
