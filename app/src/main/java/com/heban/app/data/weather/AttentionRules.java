package com.heban.app.data.weather;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.WeatherBundle;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 天气自动判断作物「需关注」（纯 Java）。返回说明文字或 null（null 表示清除天气来源的标记）。
 * <ul>
 *   <li>露天作物：今天起 3 天内最低温 ≤ 2°C →「霜冻风险」（优先）</li>
 *   <li>黄瓜：湿度 ≥ 85% →「霜霉病风险」</li>
 *   <li>番茄 / 西红柿：湿度 ≥ 85% →「灰霉病风险」</li>
 * </ul>
 * 湿度：大棚作物优先用地块 24 小时内手动记录的棚内湿度；否则用今天的预报湿度（没有时用实时湿度）。
 */
public final class AttentionRules {
    public static final String NOTE_FROST = "霜冻风险";
    public static final String NOTE_DOWNY_MILDEW = "霜霉病风险";
    public static final String NOTE_GRAY_MOLD = "灰霉病风险";

    public static final int HUMIDITY_THRESHOLD = 85;
    public static final int FROST_TEMP = 2;
    public static final int FROST_DAYS = 3;
    public static final long INDOOR_READING_MAX_AGE = 24 * 3_600_000L;

    private AttentionRules() {
    }

    @Nullable
    public static String evaluate(CropEntity crop, @Nullable PlotEntity plot, @Nullable WeatherBundle b,
                                  long now, ZoneId zone) {
        if (crop == null || b == null) return null;
        boolean greenhouse = isGreenhouse(crop, plot);
        boolean open = !greenhouse && (CropEntity.MODE_OPEN.equals(crop.plantMode)
                || (plot != null && PlotEntity.TYPE_OPEN.equals(plot.type)
                    && !CropEntity.MODE_POT.equals(crop.plantMode)));
        if (open && frostSoon(b, now, zone)) return NOTE_FROST;

        int humidity = humidity(b, greenhouse ? plot : null, now, zone);
        if (humidity >= HUMIDITY_THRESHOLD) {
            String name = crop.name == null ? "" : crop.name;
            if (name.contains("黄瓜")) return NOTE_DOWNY_MILDEW;
            if (name.contains("番茄") || name.contains("西红柿")) return NOTE_GRAY_MOLD;
        }
        return null;
    }

    static boolean isGreenhouse(CropEntity crop, @Nullable PlotEntity plot) {
        if (CropEntity.MODE_GREENHOUSE.equals(crop.plantMode)) return true;
        return plot != null && PlotEntity.TYPE_GREENHOUSE.equals(plot.type)
                && !CropEntity.MODE_OPEN.equals(crop.plantMode) && !CropEntity.MODE_POT.equals(crop.plantMode);
    }

    static boolean frostSoon(WeatherBundle b, long now, ZoneId zone) {
        LocalDate today = WeatherSlots.localDate(now, zone);
        List<DailyWeather> days = WeatherSlots.dailyFrom(b, today, zone);
        for (int i = 0; i < days.size() && i < FROST_DAYS; i++) {
            if (days.get(i).tempMin <= FROST_TEMP) return true;
        }
        return false;
    }

    /** 判断用的湿度；都没有返回 -1 */
    static int humidity(WeatherBundle b, @Nullable PlotEntity greenhousePlot, long now, ZoneId zone) {
        if (greenhousePlot != null && greenhousePlot.indoorHumidity != null && greenhousePlot.readingAt > 0
                && now - greenhousePlot.readingAt <= INDOOR_READING_MAX_AGE
                && now >= greenhousePlot.readingAt - 60_000L) {
            return greenhousePlot.indoorHumidity;
        }
        DailyWeather today = WeatherSlots.dailyFor(b, WeatherSlots.localDate(now, zone), zone);
        if (today != null) return today.humidity;
        return b.now != null ? b.now.humidity : -1;
    }
}
