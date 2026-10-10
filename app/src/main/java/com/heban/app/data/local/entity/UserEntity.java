package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 本机账号（mock 渠道）。密码只保存 PBKDF2 哈希与盐（技术准备文档 7.1 / 7.4）。 */
@Entity(tableName = "user", indices = @Index(value = "phone", unique = true))
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String phone = "";

    /** 验证码登录自动创建的账号可能还没有密码，此时为 null */
    @ColumnInfo(name = "password_hash")
    public String passwordHash;

    public String salt;

    /** 上一次使用的密码（重置密码时要求与最近使用过的密码不同） */
    @ColumnInfo(name = "prev_password_hash")
    public String prevPasswordHash;

    @ColumnInfo(name = "prev_salt")
    public String prevSalt;

    @NonNull
    public String nickname = "";

    /** 所在地区，只到区县，如「江苏省 南京市 江宁区」 */
    public String region;

    /** 种植身份：种植户 / 家庭园艺 / 农技人员 */
    public String identity;

    /** 主要作物，逗号分隔，如「番茄,黄瓜,小麦」 */
    public String crops;

    public String bio;

    /** 种植年限 */
    public int years;

    @ColumnInfo(name = "failed_attempts")
    public int failedAttempts;

    /** 连续输错 5 次后锁定到该时间（毫秒） */
    @ColumnInfo(name = "locked_until")
    public long lockedUntil;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    /** 注销时间，0 表示正常；7 天内重新登录可撤销 */
    @ColumnInfo(name = "deleted_at")
    public long deletedAt;
}
