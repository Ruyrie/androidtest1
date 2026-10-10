package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 和风天气缓存（H01 / H02），30 分钟内不重复请求（技术准备文档 8.2）。 */
@Entity(tableName = "weather_cache")
public class WeatherCacheEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "location_id")
    public String locationId = "";

    @ColumnInfo(name = "location_name")
    public String locationName;

    @ColumnInfo(name = "now_json")
    public String nowJson;

    @ColumnInfo(name = "hourly_json")
    public String hourlyJson;

    @ColumnInfo(name = "daily_json")
    public String dailyJson;

    @ColumnInfo(name = "fetched_at")
    public long fetchedAt;
}
