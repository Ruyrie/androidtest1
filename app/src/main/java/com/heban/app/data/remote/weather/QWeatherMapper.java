package com.heban.app.data.remote.weather;

import androidx.annotation.Nullable;

import com.heban.app.data.model.CityOption;
import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.HourlyWeather;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.model.WeatherNow;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** 和风天气 DTO → App 模型（时间转为毫秒；数值字段为字符串，缺失时取默认值）。 */
public final class QWeatherMapper {
    /** 和风天气时间格式 "2021-02-16T15:00+08:00"（也兼容带秒的 ISO 时间） */
    private static final DateTimeFormatter QW_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXXX");

    private QWeatherMapper() {
    }

    public static WeatherBundle toBundle(QwNowResp now, QwHourlyResp hourly, QwDailyResp daily,
                                         String locationId, String locationName,
                                         long fetchedAt, ZoneId zone) {
        WeatherBundle b = new WeatherBundle();
        b.locationId = locationId;
        b.locationName = locationName;
        b.fetchedAt = fetchedAt;
        b.now = toNow(now == null ? null : now.now);
        if (hourly != null && hourly.hourly != null) {
            for (QwHourlyResp.Hour h : hourly.hourly) {
                if (h == null) continue;
                long t = parseTime(h.fxTime);
                if (t <= 0) continue;
                HourlyWeather w = new HourlyWeather();
                w.time = t;
                w.temp = parseInt(h.temp, 0);
                w.text = h.text;
                w.icon = h.icon;
                w.pop = parseInt(h.pop, -1);
                w.windScale = h.windScale;
                w.humidity = parseInt(h.humidity, 0);
                w.precip = parseDouble(h.precip);
                b.hourly.add(w);
            }
        }
        if (daily != null && daily.daily != null) {
            for (QwDailyResp.Day d : daily.daily) {
                if (d == null) continue;
                long date = parseDate(d.fxDate, zone);
                if (date <= 0) continue;
                DailyWeather w = new DailyWeather();
                w.date = date;
                w.tempMax = parseInt(d.tempMax, 0);
                w.tempMin = parseInt(d.tempMin, 0);
                w.textDay = d.textDay;
                w.iconDay = d.iconDay;
                w.textNight = d.textNight;
                w.iconNight = d.iconNight;
                w.precip = parseDouble(d.precip);
                w.humidity = parseInt(d.humidity, 0);
                w.windScaleDay = d.windScaleDay;
                b.daily.add(w);
            }
        }
        return b;
    }

    @Nullable
    public static WeatherNow toNow(@Nullable QwNowResp.Now n) {
        if (n == null) return null;
        WeatherNow w = new WeatherNow();
        w.temp = parseInt(n.temp, 0);
        w.feelsLike = parseInt(n.feelsLike, w.temp);
        w.text = n.text;
        w.icon = n.icon;
        w.windDir = n.windDir;
        w.windScale = n.windScale;
        w.humidity = parseInt(n.humidity, 0);
        w.precip = parseDouble(n.precip);
        w.obsTime = parseTime(n.obsTime);
        return w;
    }

    public static List<CityOption> toCities(@Nullable QwCityResp resp) {
        List<CityOption> out = new ArrayList<>();
        if (resp == null || resp.location == null) return out;
        for (QwCityResp.Location l : resp.location) {
            if (l == null || l.id == null || l.id.isEmpty()) continue;
            CityOption c = new CityOption();
            c.id = l.id;
            c.name = l.name;
            c.adm2 = l.adm2;
            c.adm1 = l.adm1;
            out.add(c);
        }
        return out;
    }

    /** "2021-02-16T15:00+08:00" → 毫秒；无法解析返回 0 */
    public static long parseTime(@Nullable String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return OffsetDateTime.parse(s, QW_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(s).toInstant().toEpochMilli();
            } catch (DateTimeParseException e2) {
                return 0;
            }
        }
    }

    /** "2021-11-15" → 当天 0 点（zone 时区）的毫秒；无法解析返回 0 */
    public static long parseDate(@Nullable String s, ZoneId zone) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return LocalDate.parse(s).atStartOfDay(zone).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    static int parseInt(@Nullable String s, int def) {
        if (s == null) return def;
        String v = s.trim();
        if (v.isEmpty()) return def;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            try {
                return (int) Math.round(Double.parseDouble(v));
            } catch (NumberFormatException e2) {
                return def;
            }
        }
    }

    static double parseDouble(@Nullable String s) {
        if (s == null) return 0;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
