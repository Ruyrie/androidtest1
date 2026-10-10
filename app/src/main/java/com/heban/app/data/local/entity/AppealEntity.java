package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 手机号停用后的申诉换绑记录（A09）。 */
@Entity(tableName = "appeal")
public class AppealEntity {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 申诉编号，如 HB20261010-0832 */
    @NonNull
    @ColumnInfo(name = "appeal_no")
    public String appealNo = "";

    @NonNull
    @ColumnInfo(name = "old_phone")
    public String oldPhone = "";

    public String nickname;

    /** 最近一次登录：一周内 / 一个月内 / 三个月内 / 更早 */
    @ColumnInfo(name = "last_login")
    public String lastLogin;

    /** 常用登录地区，如「江苏 南京」 */
    public String region;

    @NonNull
    @ColumnInfo(name = "new_phone")
    public String newPhone = "";

    @NonNull
    public String status = STATUS_PENDING;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
