package com.heban.app.data.weather;

import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.HourlyWeather;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.model.WeatherNow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 与原型 H01 / H02 一致的天气数据：2026-10-10（周六）15:02，南京 江宁，24°C 多云，湿度 68%，东南风 2 级；
 * 7 天 18/26、17/23、8/15（雨）、7/14、8/17、10/19、11/20；今夜到明晨有小雨约 6 mm。
 */
final class WeatherFixtures {
    static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private WeatherFixtures() {
    }

    static long at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 10, day, hour, minute).atZone(ZONE).toInstant().toEpochMilli();
    }

    /** 原型时刻 10 日 15:02 */
    static long now() {
        return at(10, 15, 2);
    }

    static WeatherBundle prototype() {
        WeatherBundle b = new WeatherBundle();
        b.locationId = "101190104";
        b.locationName = "南京 江宁";
        b.fetchedAt = at(10, 15, 2);

        WeatherNow n = new WeatherNow();
        n.temp = 24;
        n.feelsLike = 25;
        n.text = "多云";
        n.icon = "101";
        n.windDir = "东南风";
        n.windScale = "2";
        n.humidity = 68;
        n.precip = 0;
        n.obsTime = at(10, 15, 0);
        b.now = n;

        // {day, hour, temp, icon, wind, pop, humidity, precip×10}
        int[][] hours = {
                {10, 16, 25, 100, 2, 0, 60, 0},
                {10, 17, 24, 100, 2, 0, 62, 0},
                {10, 18, 22, 104, 4, 10, 66, 0},
                {10, 19, 21, 150, 3, 10, 70, 0},
                {10, 20, 20, 150, 3, 10, 75, 0},
                {10, 21, 19, 151, 2, 20, 78, 0},
                {10, 22, 19, 104, 2, 40, 82, 0},
                {10, 23, 18, 104, 2, 60, 84, 0},
                {11, 0, 18, 305, 2, 80, 90, 5},
                {11, 1, 18, 305, 2, 85, 92, 10},
                {11, 2, 18, 305, 2, 90, 95, 15},
                {11, 3, 18, 305, 2, 90, 95, 15},
                {11, 4, 17, 305, 2, 80, 95, 10},
                {11, 5, 17, 305, 1, 60, 93, 5},
                {11, 6, 17, 104, 1, 40, 90, 0},
                {11, 7, 18, 101, 1, 10, 88, 0},
                {11, 8, 19, 101, 1, 10, 86, 0},
                {11, 9, 20, 101, 1, 5, 80, 0},
                {11, 10, 21, 101, 4, 5, 75, 0},
                {11, 11, 22, 101, 4, 5, 70, 0},
                {11, 12, 23, 104, 4, 10, 68, 0},
                {11, 13, 23, 104, 4, 10, 68, 0},
                {11, 14, 22, 104, 4, 10, 70, 0},
                {11, 15, 21, 104, 4, 10, 72, 0},
        };
        for (int[] h : hours) b.hourly.add(hour(h[0], h[1], h[2], String.valueOf(h[3]), String.valueOf(h[4]),
                h[5], h[6], h[7] / 10.0));

        b.daily.add(day(10, 26, 18, "101", "多云", 0, 72, "1-2"));
        b.daily.add(day(11, 23, 17, "104", "阴", 6.0, 80, "1-2"));
        b.daily.add(day(12, 15, 8, "305", "小雨", 8.0, 90, "3-4"));
        b.daily.add(day(13, 14, 7, "104", "阴", 0, 75, "3-4"));
        b.daily.add(day(14, 17, 8, "100", "晴", 0, 60, "1-2"));
        b.daily.add(day(15, 19, 10, "100", "晴", 0, 58, "1-2"));
        b.daily.add(day(16, 20, 11, "101", "多云", 0, 62, "1-2"));
        return b;
    }

    static HourlyWeather hour(int day, int hour, int temp, String icon, String wind, int pop, int humidity,
                              double precip) {
        HourlyWeather w = new HourlyWeather();
        w.time = at(day, hour, 0);
        w.temp = temp;
        w.icon = icon;
        w.text = icon.startsWith("3") ? "小雨" : "多云";
        w.windScale = wind;
        w.pop = pop;
        w.humidity = humidity;
        w.precip = precip;
        return w;
    }

    static DailyWeather day(int day, int max, int min, String icon, String text, double precip, int humidity,
                            String wind) {
        DailyWeather d = new DailyWeather();
        d.date = LocalDate.of(2026, 10, day).atStartOfDay(ZONE).toInstant().toEpochMilli();
        d.tempMax = max;
        d.tempMin = min;
        d.iconDay = icon;
        d.textDay = text;
        d.precip = precip;
        d.humidity = humidity;
        d.windScaleDay = wind;
        return d;
    }

    static DailyWeather daily(WeatherBundle b, int dayOfMonth) {
        for (DailyWeather d : b.daily) {
            if (WeatherSlots.localDate(d.date, ZONE).getDayOfMonth() == dayOfMonth) return d;
        }
        throw new IllegalArgumentException("no day " + dayOfMonth);
    }

    static HourlyWeather hourly(WeatherBundle b, int dayOfMonth, int hour) {
        long t = at(dayOfMonth, hour, 0);
        for (HourlyWeather h : b.hourly) {
            if (h.time == t) return h;
        }
        throw new IllegalArgumentException("no hour " + dayOfMonth + " " + hour);
    }
}
