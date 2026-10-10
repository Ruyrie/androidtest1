package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 收货地址（P05 / P06）。每个账号最多 10 个，默认地址唯一。 */
@Entity(tableName = "address", indices = @Index("user_id"))
public class AddressEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    public String name = "";

    @NonNull
    public String phone = "";

    /** 所在地区，如「江苏省 南京市 江宁区」 */
    @NonNull
    public String region = "";

    @NonNull
    public String detail = "";

    @ColumnInfo(name = "is_default")
    public boolean isDefault;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
