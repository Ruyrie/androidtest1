package com.heban.app.data.weather;

import androidx.annotation.Nullable;

import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.HourlyWeather;
import com.heban.app.data.model.WeatherBundle;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 预报数据的整理工具：逐小时时段、按日期取逐日预报、风力等级解析、天气现象判断（纯 Java）。 */
final class WeatherSlots {
    static final long HOUR = 3_600_000L;

    private WeatherSlots() {
    }

    /** 一个整点时段（逐小时预报，或由实时天气补出的当前小时） */
    static final class Slot {
        long start;
        LocalDateTime local;
        int temp;
        /** 降水概率 0–100（缺失时按降水量推断） */
        int pop;
        int windLo;
        int windHi;
        double precip;
        int humidity;
        String icon;
        /** 由实时天气补出的当前小时（降水量是过去 1 小时的实况，不计入未来降水量） */
        boolean synthetic;

        LocalDate date() {
            return local.toLocalDate();
        }

        int hour() {
            return local.getHour();
        }
    }

    static long floorHour(long millis, ZoneId zone) {
        return Instant.ofEpochMilli(millis).atZone(zone).truncatedTo(ChronoUnit.HOURS)
                .toInstant().toEpochMilli();
    }

    static LocalDate localDate(long millis, ZoneId zone) {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
    }

    /**
     * 从当前小时开始的逐小时时段。逐小时预报一般从下一个整点开始，
     * 这时用实时天气补出当前小时（降水概率取实况有降水 → 100，否则取下一小时的概率）。
     */
    static List<Slot> slots(@Nullable WeatherBundle b, long now, ZoneId zone) {
        List<Slot> out = new ArrayList<>();
        if (b == null) return out;
        long hourStart = floorHour(now, zone);
        List<HourlyWeather> hourly = new ArrayList<>(b.hourly == null
                ? Collections.<HourlyWeather>emptyList() : b.hourly);
        Collections.sort(hourly, (x, y) -> Long.compare(x.time, y.time));
        for (HourlyWeather h : hourly) {
            if (h == null || h.time < hourStart) continue;
            Slot s = new Slot();
            s.start = floorHour(h.time, zone);
            s.local = LocalDateTime.ofInstant(Instant.ofEpochMilli(s.start), zone);
            s.temp = h.temp;
            s.precip = h.precip;
            s.pop = h.pop >= 0 ? h.pop : (h.precip > 0 ? 100 : 0);
            int[] w = windRange(h.windScale);
            s.windLo = w[0];
            s.windHi = w[1];
            s.humidity = h.humidity;
            s.icon = h.icon;
            out.add(s);
        }
        if (b.now != null && (out.isEmpty() || out.get(0).start > hourStart)) {
            long obs = b.now.obsTime > 0 ? b.now.obsTime : b.fetchedAt;
            if (Math.abs(now - obs) <= 3 * HOUR) {
                Slot s = new Slot();
                s.start = hourStart;
                s.local = LocalDateTime.ofInstant(Instant.ofEpochMilli(hourStart), zone);
                s.temp = b.now.temp;
                s.precip = b.now.precip;
                s.pop = b.now.precip > 0 ? 100 : (out.isEmpty() ? 0 : out.get(0).pop);
                int[] w = windRange(b.now.windScale);
                s.windLo = w[0];
                s.windHi = w[1];
                s.humidity = b.now.humidity;
                s.icon = b.now.icon;
                s.synthetic = true;
                out.add(0, s);
            }
        }
        return out;
    }

    /** 逐日预报中某一天（按 zone 时区的日期匹配） */
    @Nullable
    static DailyWeather dailyFor(@Nullable WeatherBundle b, LocalDate day, ZoneId zone) {
        if (b == null || b.daily == null) return null;
        for (DailyWeather d : b.daily) {
            if (d != null && localDate(d.date, zone).equals(day)) return d;
        }
        return null;
    }

    /** 从 day 起（含）按日期排序的逐日预报 */
    static List<DailyWeather> dailyFrom(@Nullable WeatherBundle b, LocalDate day, ZoneId zone) {
        List<DailyWeather> out = new ArrayList<>();
        if (b == null || b.daily == null) return out;
        for (DailyWeather d : b.daily) {
            if (d != null && !localDate(d.date, zone).isBefore(day)) out.add(d);
        }
        Collections.sort(out, (x, y) -> Long.compare(x.date, y.date));
        return out;
    }

    /** 风力等级 "2" → [2, 2]，"3-4" → [3, 4]；无法解析为 [0, 0] */
    static int[] windRange(@Nullable String scale) {
        if (scale == null) return new int[]{0, 0};
        String s = scale.trim();
        if (s.isEmpty()) return new int[]{0, 0};
        String[] parts = s.split("[-–~]");
        int lo = Integer.MAX_VALUE;
        int hi = Integer.MIN_VALUE;
        for (String p : parts) {
            try {
                int v = Integer.parseInt(p.trim());
                lo = Math.min(lo, v);
                hi = Math.max(hi, v);
            } catch (NumberFormatException ignored) {
                // 跳过
            }
        }
        if (lo == Integer.MAX_VALUE) return new int[]{0, 0};
        return new int[]{lo, hi};
    }

    /** 「风力 1 级」/「风力 1–2 级」 */
    static String windText(int lo, int hi) {
        return lo == hi ? "风力 " + lo + " 级" : "风力 " + lo + "–" + hi + " 级";
    }

    static int iconCode(@Nullable String icon) {
        if (icon == null) return -1;
        try {
            return Integer.parseInt(icon.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 晴 / 多云 / 少云 / 晴间多云（含夜间） */
    static boolean isSunnyOrCloudy(@Nullable String icon, @Nullable String text) {
        int c = iconCode(icon);
        if ((c >= 100 && c <= 103) || (c >= 150 && c <= 153)) return true;
        if (c >= 0) return false;
        return text != null && (text.contains("晴") || text.contains("多云") || text.contains("少云"));
    }

    /** 雨（含雨夹雪等） */
    static boolean isRain(@Nullable String icon, @Nullable String text) {
        int c = iconCode(icon);
        if ((c >= 300 && c <= 399) || c == 404 || c == 405 || c == 406 || c == 456) return true;
        return text != null && text.contains("雨");
    }

    static boolean isSnow(@Nullable String icon, @Nullable String text) {
        int c = iconCode(icon);
        if (c >= 400 && c <= 499) return true;
        return text != null && text.contains("雪");
    }

    /** 中雨及以上（强阵雨、强雷阵雨、中雨、大雨、暴雨、小到中雨……） */
    static boolean isModerateRainIcon(@Nullable String icon) {
        switch (iconCode(icon)) {
            case 301: case 303: case 304: case 306: case 307: case 308: case 310: case 311:
            case 312: case 314: case 315: case 316: case 317: case 318: case 351:
                return true;
            default:
                return false;
        }
    }

    /** 当天有降水（雨或雪） */
    static boolean hasPrecip(@Nullable DailyWeather d) {
        return d != null && (d.precip > 0 || isRain(d.iconDay, d.textDay) || isSnow(d.iconDay, d.textDay));
    }

    /** "6" / "5.5" */
    static String mm(double v) {
        double r = Math.round(v * 10) / 10.0;
        if (r == Math.rint(r)) return String.valueOf((long) r);
        return String.valueOf(r);
    }
}
