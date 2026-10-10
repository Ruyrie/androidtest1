package com.heban.app.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

/** 当前账号关注的农友（S01 / S06 / S07）。 */
@Entity(tableName = "follow", primaryKeys = {"user_id", "farmer_id"})
public class FollowEntity {
    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "farmer_id")
    public long farmerId;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
