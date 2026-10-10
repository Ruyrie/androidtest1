package com.heban.app.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

/** 关注了当前账号的农友（S07「粉丝」）。新账号为空；农友回关等模拟互动时写入。 */
@Entity(tableName = "fan", primaryKeys = {"user_id", "farmer_id"})
public class FanEntity {
    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "farmer_id")
    public long farmerId;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
