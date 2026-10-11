package com.heban.app.data.weather;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.heban.app.data.local.entity.WeatherCacheEntity;
import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.HourlyWeather;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.model.WeatherNow;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** WeatherBundle ↔ weather_cache 行（三段 JSON）。 */
public final class WeatherCacheMapper {
    private static final Type HOURLY = new TypeToken<List<HourlyWeather>>() { }.getType();
    private static final Type DAILY = new TypeToken<List<DailyWeather>>() { }.getType();

    private WeatherCacheMapper() {
    }

    public static WeatherCacheEntity toEntity(WeatherBundle b, Gson gson) {
        WeatherCacheEntity e = new WeatherCacheEntity();
        e.locationId = b.locationId == null ? "" : b.locationId;
        e.locationName = b.locationName;
        e.nowJson = b.now == null ? null : gson.toJson(b.now);
        e.hourlyJson = gson.toJson(b.hourly == null ? new ArrayList<HourlyWeather>() : b.hourly, HOURLY);
        e.dailyJson = gson.toJson(b.daily == null ? new ArrayList<DailyWeather>() : b.daily, DAILY);
        e.fetchedAt = b.fetchedAt;
        return e;
    }

    /** 缓存内容损坏时返回 null */
    @Nullable
    public static WeatherBundle fromEntity(@Nullable WeatherCacheEntity e, Gson gson) {
        if (e == null) return null;
        try {
            WeatherBundle b = new WeatherBundle();
            b.locationId = e.locationId;
            b.locationName = e.locationName;
            b.fetchedAt = e.fetchedAt;
            b.now = e.nowJson == null ? null : gson.fromJson(e.nowJson, WeatherNow.class);
            List<HourlyWeather> hourly = e.hourlyJson == null ? null : gson.fromJson(e.hourlyJson, HOURLY);
            List<DailyWeather> daily = e.dailyJson == null ? null : gson.fromJson(e.dailyJson, DAILY);
            if (hourly != null) b.hourly = hourly;
            if (daily != null) b.daily = daily;
            return b;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
