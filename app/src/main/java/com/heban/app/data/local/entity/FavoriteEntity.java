package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 收藏（D05 病害、D08 商品、S02 动态；M04 我的收藏）。按账号隔离。 */
@Entity(tableName = "favorite",
        indices = @Index(value = {"user_id", "target_type", "target_id"}, unique = true))
public class FavoriteEntity {
    public static final String TYPE_PEST = "pest";
    public static final String TYPE_PRODUCT = "product";
    public static final String TYPE_POST = "post";

    @PrimaryKey(autoGenerate = true)
    public long id;

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
