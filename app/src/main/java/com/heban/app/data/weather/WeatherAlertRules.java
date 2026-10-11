package com.heban.app.data.weather;

import androidx.annotation.Nullable;

import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.WeatherAlert;
import com.heban.app.data.model.WeatherBundle;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * App 本地计算的天气预警（技术准备文档表 8-3）。纯 Java。
 *
 * <p>降温预警：未来 3 天中某天最低温比今天低 ≥ 8°C，或最低温 ≤ 8°C 且降幅 ≥ 5°C。
 * 第一个满足的日期为起始日；之后最低温不高于起始日最低温 + 1°C 的连续日期算作受冷空气影响的时段。
 * H01：「12日起最低 8°C，注意大棚保温」（起始日的最低温）；
 * H02 / 通知：「12–14 日受冷空气影响，最低 7°C，大棚夜间注意加盖保温被」（时段内的最低温）。
 */
public final class WeatherAlertRules {
    public static final String TITLE_COOLING = "降温预警";
    public static final int COOLING_LOOKAHEAD_DAYS = 3;
    public static final int COOLING_DROP = 8;
    public static final int COOLING_LOW_TEMP = 8;
    public static final int COOLING_LOW_DROP = 5;
    /** 冷空气时段：最低温不高于起始日最低温 + 1°C */
    public static final int COLD_SPELL_TOLERANCE = 1;

    private WeatherAlertRules() {
    }

    /** 当前需要显示的预警；没有返回 null */
    @Nullable
    public static WeatherAlert evaluate(@Nullable WeatherBundle b, long now, ZoneId zone) {
        return cooling(b, WeatherSlots.localDate(now, zone), zone);
    }

    @Nullable
    static WeatherAlert cooling(@Nullable WeatherBundle b, LocalDate today, ZoneId zone) {
        List<DailyWeather> days = WeatherSlots.dailyFrom(b, today, zone);
        if (days.size() < 2) return null;
        DailyWeather first = days.get(0);
        if (!WeatherSlots.localDate(first.date, zone).equals(today)) return null;
        int todayMin = first.tempMin;
        int start = -1;
        for (int i = 1; i < days.size() && i <= COOLING_LOOKAHEAD_DAYS; i++) {
            int min = days.get(i).tempMin;
            int drop = todayMin - min;
            if (drop >= COOLING_DROP || (min <= COOLING_LOW_TEMP && drop >= COOLING_LOW_DROP)) {
                start = i;
                break;
            }
        }
        if (start < 0) return null;
        int startMin = days.get(start).tempMin;
        int end = start;
        int spanMin = startMin;
        while (end + 1 < days.size() && days.get(end + 1).tempMin <= startMin + COLD_SPELL_TOLERANCE) {
            end++;
            spanMin = Math.min(spanMin, days.get(end).tempMin);
        }
        LocalDate s = WeatherSlots.localDate(days.get(start).date, zone);
        LocalDate e = WeatherSlots.localDate(days.get(end).date, zone);
        String span;
        if (start == end) span = s.getDayOfMonth() + " 日";
        else if (s.getMonthValue() == e.getMonthValue()) span = s.getDayOfMonth() + "–" + e.getDayOfMonth() + " 日";
        else span = s.getDayOfMonth() + " 日–" + e.getDayOfMonth() + " 日";

        WeatherAlert a = new WeatherAlert();
        a.title = TITLE_COOLING;
        a.shortText = s.getDayOfMonth() + "日起最低 " + startMin + "°C，注意大棚保温";
        a.detailText = span + "受冷空气影响，最低 " + spanMin + "°C，大棚夜间注意加盖保温被";
        return a;
    }
}
