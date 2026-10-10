package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

/** 签到（H05）。每个账号每天一条。 */
@Entity(tableName = "checkin", primaryKeys = {"user_id", "date"})
public class CheckinEntity {
    @ColumnInfo(name = "user_id")
    public long userId;

    /** yyyy-MM-dd */
    @NonNull
    public String date = "";

    /** 截至当天的连续签到天数 */
    public int streak;

    /** 当天签到获得的积分（含满 7 天奖励） */
    public int points;
}
