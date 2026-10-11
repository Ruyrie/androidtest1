package com.heban.app.data.weather;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.R;
import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.FarmIndex;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.weather.WeatherSlots.Slot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 农事指数（H02 六宫格）、H01 农事建议、H04「AI 提示」——按技术准备文档表 8-3 在本地计算，不调用大模型。纯 Java。
 *
 * <p>喷药：每个整点按「降水概率 &lt; 30%、风力 ≤ 3 级、气温 10–30°C → 适宜；风力 4 级或降水概率 30–60%（其余满足）
 * → 较适宜；否则不宜」分级。「未来 6 小时」只看作业时段 6:00–19:00 内的小时；夜间则看次日作业时段的前 6 小时。
 * 指数取其中最好的等级，连续适宜的小时作为推荐时段（如「15:00–18:00」）。
 */
public final class FarmIndexCalculator {
    // —— 喷药 ——
    public static final int SPRAY_POP_MAX = 30;
    public static final int SPRAY_POP_FAIR_MAX = 60;
    public static final int SPRAY_WIND_MAX = 3;
    public static final int SPRAY_WIND_FAIR_MAX = 4;
    public static final int SPRAY_TEMP_MIN = 10;
    public static final int SPRAY_TEMP_MAX = 30;
    public static final int SPRAY_LOOKAHEAD_HOURS = 6;
    /** 作业时段 [6:00, 19:00) */
    public static final int WORK_START_HOUR = 6;
    public static final int WORK_END_HOUR = 19;
    // —— 灌溉 / 施肥 ——
    public static final double IRRIGATE_RAIN_MM = 5;
    public static final int IRRIGATE_DRY_HUMIDITY = 50;
    /** 中雨：24 小时 ≥ 10 mm，或 1 小时 ≥ 2.6 mm，或天气现象为中雨及以上 */
    public static final double MODERATE_RAIN_DAY_MM = 10;
    public static final double MODERATE_RAIN_HOUR_MM = 2.6;
    // —— 晾晒 / 霜冻 / 通风 ——
    public static final int DRY_HUMIDITY = 70;
    public static final int DRY_HUMIDITY_FAIR_MAX = 85;
    public static final int FROST_TEMP = 2;
    public static final int VENT_HUMIDITY = 85;
    /** H04 提示中「建议提前完成」看之后几天内的降雨 */
    public static final int RAIN_AHEAD_DAYS = 3;

    public static final String NAME_SPRAY = "喷药";
    public static final String NAME_IRRIGATE = "灌溉";
    public static final String NAME_FERTILIZE = "施肥";
    public static final String NAME_DRY = "晾晒";
    public static final String NAME_FROST = "霜冻";
    public static final String NAME_VENT = "通风";

    public static final String LEVEL_SUITABLE = "适宜";
    public static final String LEVEL_FAIR = "较适宜";
    public static final String LEVEL_UNSUITABLE = "不宜";
    public static final String LEVEL_NOT_NEEDED = "暂不需要";
    public static final String LEVEL_DEPENDS = "视墒情";
    public static final String LEVEL_MORNING = "上午为宜";
    public static final String LEVEL_NIGHT_DEHUMIDIFY = "夜间排湿";
    public static final String LEVEL_NO_RISK = "无风险";
    public static final String LEVEL_NO_DATA = "暂无数据";

    enum Suit { GOOD, FAIR, BAD }

    /** 连续的作业时段 */
    static final class Window {
        final List<Slot> slots = new ArrayList<>();

        long start() {
            return slots.get(0).start;
        }

        long end() {
            return slots.get(slots.size() - 1).start + WeatherSlots.HOUR;
        }

        int windLo() {
            int v = Integer.MAX_VALUE;
            for (Slot s : slots) v = Math.min(v, s.windLo);
            return v;
        }

        int windHi() {
            int v = 0;
            for (Slot s : slots) v = Math.max(v, s.windHi);
            return v;
        }

        boolean dry() {
            for (Slot s : slots) if (s.precip > 0) return false;
            return true;
        }
    }

    private FarmIndexCalculator() {
    }

    // ———————————————————————— H02 六项指数 ————————————————————————

    /** 喷药 / 灌溉 / 施肥 / 晾晒 / 霜冻 / 通风（没有数据时返回空列表） */
    @NonNull
    public static List<FarmIndex> indices(@Nullable WeatherBundle b, long now, ZoneId zone) {
        List<FarmIndex> out = new ArrayList<>();
        if (b == null || (b.now == null && b.hourly.isEmpty() && b.daily.isEmpty())) return out;
        List<Slot> slots = WeatherSlots.slots(b, now, zone);
        LocalDate today = WeatherSlots.localDate(now, zone);
        out.add(spray(b, slots, now, zone));
        out.add(irrigate(b, slots, now, zone));
        out.add(fertilize(b, slots, now, zone));
        out.add(dry(b, today, zone));
        out.add(frost(b, today, zone));
        out.add(vent(b, slots, now, zone));
        return out;
    }

    static FarmIndex spray(WeatherBundle b, List<Slot> slots, long now, ZoneId zone) {
        List<Slot> cand = sprayCandidates(slots, now, zone);
        if (cand.isEmpty()) {
            DailyWeather d = WeatherSlots.dailyFor(b, WeatherSlots.localDate(now, zone), zone);
            if (d == null) return index(NAME_SPRAY, LEVEL_NO_DATA, null, R.drawable.ic_sanitizer, false);
            return index(NAME_SPRAY, levelOf(classifyDaily(d)), null, R.drawable.ic_sanitizer, false);
        }
        Suit best = Suit.BAD;
        for (Slot s : cand) {
            Suit c = classify(s);
            if (c.ordinal() < best.ordinal()) best = c;
        }
        String detail = null;
        if (best == Suit.GOOD) {
            Window w = sprayWindow(slots, now, zone);
            if (w != null) detail = windowText(w, now, zone);
        }
        return index(NAME_SPRAY, levelOf(best), detail, R.drawable.ic_sanitizer, false);
    }

    static FarmIndex irrigate(WeatherBundle b, List<Slot> slots, long now, ZoneId zone) {
        double rain = rain24(b, slots, now, zone);
        int humidity = currentHumidity(b, now, zone);
        if (rain >= IRRIGATE_RAIN_MM) {
            return index(NAME_IRRIGATE, LEVEL_NOT_NEEDED, "未来 24 小时降水约 " + WeatherSlots.mm(rain) + " mm",
                    R.drawable.ic_water_drop, false);
        }
        if (humidity >= 0 && humidity < IRRIGATE_DRY_HUMIDITY && rain <= 0) {
            return index(NAME_IRRIGATE, LEVEL_SUITABLE, "空气干燥、无降水", R.drawable.ic_water_drop, false);
        }
        return index(NAME_IRRIGATE, LEVEL_DEPENDS, null, R.drawable.ic_water_drop, false);
    }

    static FarmIndex fertilize(WeatherBundle b, List<Slot> slots, long now, ZoneId zone) {
        boolean moderate = moderateRain24(b, slots, now, zone);
        return index(NAME_FERTILIZE, moderate ? LEVEL_UNSUITABLE : LEVEL_SUITABLE,
                moderate ? "未来 24 小时有中雨以上降水" : "未来 24 小时无中雨以上降水",
                R.drawable.ic_compost, false);
    }

    static FarmIndex dry(WeatherBundle b, LocalDate today, ZoneId zone) {
        DailyWeather d = WeatherSlots.dailyFor(b, today, zone);
        if (d == null) return index(NAME_DRY, LEVEL_NO_DATA, null, R.drawable.ic_wb_sunny, false);
        String level;
        if (WeatherSlots.isRain(d.iconDay, d.textDay) || WeatherSlots.isSnow(d.iconDay, d.textDay)) {
            level = LEVEL_UNSUITABLE;
        } else if (WeatherSlots.isSunnyOrCloudy(d.iconDay, d.textDay) && d.humidity < DRY_HUMIDITY) {
            level = LEVEL_SUITABLE;
        } else if (d.humidity <= DRY_HUMIDITY_FAIR_MAX) {
            level = LEVEL_FAIR;
        } else {
            level = LEVEL_UNSUITABLE;
        }
        return index(NAME_DRY, level, "湿度 " + d.humidity + "%", R.drawable.ic_wb_sunny, false);
    }

    static FarmIndex frost(WeatherBundle b, LocalDate today, ZoneId zone) {
        List<DailyWeather> days = WeatherSlots.dailyFrom(b, today, zone);
        if (days.isEmpty()) return index(NAME_FROST, LEVEL_NO_DATA, null, R.drawable.ic_severe_cold, false);
        for (int i = 0; i < days.size() && i < 7; i++) {
            DailyWeather d = days.get(i);
            if (d.tempMin <= FROST_TEMP) {
                int dom = WeatherSlots.localDate(d.date, zone).getDayOfMonth();
                return index(NAME_FROST, dom + "日起注意", "最低 " + d.tempMin + "°C",
                        R.drawable.ic_severe_cold, true);
            }
        }
        return index(NAME_FROST, LEVEL_NO_RISK, null, R.drawable.ic_severe_cold, false);
    }

    static FarmIndex vent(WeatherBundle b, List<Slot> slots, long now, ZoneId zone) {
        boolean dayHigh = false;
        boolean nightHigh = false;
        if (b.now != null && b.now.humidity >= VENT_HUMIDITY) dayHigh = true;
        DailyWeather today = WeatherSlots.dailyFor(b, WeatherSlots.localDate(now, zone), zone);
        if (today != null && today.humidity >= VENT_HUMIDITY) dayHigh = true;
        long end = WeatherSlots.floorHour(now, zone) + 24 * WeatherSlots.HOUR;
        for (Slot s : slots) {
            if (s.start >= end || s.humidity < VENT_HUMIDITY) continue;
            if (isWorkHour(s)) dayHigh = true;
            else nightHigh = true;
        }
        if (dayHigh) return index(NAME_VENT, LEVEL_MORNING, "湿度偏高，上午通风降湿", R.drawable.ic_air, false);
        if (nightHigh) {
            return index(NAME_VENT, LEVEL_NIGHT_DEHUMIDIFY, "夜间湿度高，注意排湿", R.drawable.ic_air, false);
        }
        return index(NAME_VENT, LEVEL_SUITABLE, null, R.drawable.ic_air, false);
    }

    // ———————————————————————— H01 农事建议 ————————————————————————

    /**
     * H01 天气卡底部的农事建议，如「15:00–18:00 风力小，适宜喷药」。
     * 依次取：喷药推荐时段 → 降雨不需灌溉 → 通风 → 空气干燥宜灌溉 → 不宜喷药；都不满足返回 null。
     */
    @Nullable
    public static String farmTip(@Nullable WeatherBundle b, long now, ZoneId zone) {
        List<FarmIndex> list = indices(b, now, zone);
        if (list.isEmpty()) return null;
        FarmIndex spray = list.get(0);
        FarmIndex irrigate = list.get(1);
        FarmIndex vent = list.get(5);
        if (LEVEL_SUITABLE.equals(spray.level) && spray.detail != null) {
            return spray.detail + " 风力小，适宜喷药";
        }
        if (LEVEL_NOT_NEEDED.equals(irrigate.level)) return "未来 24 小时有降雨，暂不需要灌溉";
        if (LEVEL_MORNING.equals(vent.level)) return "湿度偏高，上午适宜通风";
        if (LEVEL_NIGHT_DEHUMIDIFY.equals(vent.level)) return "夜间湿度高，注意排湿";
        if (LEVEL_SUITABLE.equals(irrigate.level)) return "空气干燥，适宜灌溉";
        if (LEVEL_UNSUITABLE.equals(spray.level)) return "风大或有雨，暂不宜喷药";
        return null;
    }

    // ———————————————————————— H04 AI 提示 ————————————————————————

    /**
     * H04「AI 提示」（不含前缀），如「11 日 7:00–10:00 风力 1 级、无降水，适合喷药；12 日起有降雨，建议提前完成。」
     * 有逐小时预报时在当天作业时段内找最长的适宜时段，否则按逐日预报判断。没有数据或日期已过返回 ""。
     */
    @NonNull
    public static String farmTaskTip(@Nullable WeatherBundle b, @Nullable FarmType type, @Nullable LocalDate day,
                                     long now, ZoneId zone) {
        if (b == null || type == null || day == null) return "";
        LocalDate today = WeatherSlots.localDate(now, zone);
        if (day.isBefore(today)) return "";
        DailyWeather d = WeatherSlots.dailyFor(b, day, zone);
        List<Slot> daySlots = new ArrayList<>();
        for (Slot s : WeatherSlots.slots(b, now, zone)) {
            if (s.date().equals(day) && isWorkHour(s)) daySlots.add(s);
        }
        if (d == null && daySlots.isEmpty()) return "";
        String dd = day.getDayOfMonth() + " 日";
        switch (type) {
            case SPRAY:
                return sprayTip(b, d, daySlots, day, dd, zone);
            case FERTILIZE: {
                boolean moderate = d != null
                        && (d.precip >= MODERATE_RAIN_DAY_MM || WeatherSlots.isModerateRainIcon(d.iconDay));
                for (Slot s : daySlots) {
                    if (s.precip >= MODERATE_RAIN_HOUR_MM || WeatherSlots.isModerateRainIcon(s.icon)) moderate = true;
                }
                return moderate ? dd + "有中雨以上降水，不宜施肥，建议雨后再施。"
                                : dd + "无中雨以上降水，适合施肥。";
            }
            case IRRIGATE: {
                if (d == null) return "";
                if (d.precip >= IRRIGATE_RAIN_MM) {
                    return dd + "预计降水 " + WeatherSlots.mm(d.precip) + " mm，暂不需要灌溉。";
                }
                boolean wet = WeatherSlots.hasPrecip(d);
                if (!wet && d.humidity < IRRIGATE_DRY_HUMIDITY) return dd + "空气干燥、无降水，适合灌溉，宜在早晚进行。";
                return dd + (wet ? "降水较少" : "无明显降水") + "，请视墒情灌溉。";
            }
            case PRUNE:
                if (d == null) return "";
                return WeatherSlots.hasPrecip(d) ? dd + "有降水，伤口不易愈合，建议改在晴天整枝。"
                                                 : dd + "无降水，适合整枝。";
            case HARVEST: {
                if (d == null) return "";
                if (WeatherSlots.hasPrecip(d)) return dd + "有降水，建议雨前采收或改期。";
                return dd + "无降水，适合采收" + rainAheadSuffix(b, day, zone);
            }
            case OTHER:
            default: {
                if (d == null) return "";
                String text = d.textDay == null ? "" : d.textDay;
                return dd + text + "，" + d.tempMin + "–" + d.tempMax + "°C，"
                        + (WeatherSlots.hasPrecip(d) ? "有降水。" : "无降水。");
            }
        }
    }

    private static String sprayTip(WeatherBundle b, @Nullable DailyWeather d, List<Slot> daySlots,
                                   LocalDate day, String dd, ZoneId zone) {
        if (!daySlots.isEmpty()) {
            Window good = longestRun(daySlots, Suit.GOOD);
            if (good != null) {
                return dd + " " + range(good, zone) + " "
                        + WeatherSlots.windText(good.windLo(), good.windHi()) + "、"
                        + (good.dry() ? "无降水" : "降水概率低") + "，适合喷药" + rainAheadSuffix(b, day, zone);
            }
            Window fair = longestRun(daySlots, Suit.FAIR);
            if (fair != null) {
                return dd + " " + range(fair, zone) + " "
                        + WeatherSlots.windText(fair.windLo(), fair.windHi())
                        + "，可以喷药，注意风力和降水变化" + rainAheadSuffix(b, day, zone);
            }
            return dd + "风力较大或有降水，不宜喷药，建议改期。";
        }
        if (d == null) return "";
        int[] w = WeatherSlots.windRange(d.windScaleDay);
        switch (classifyDaily(d)) {
            case GOOD:
                return dd + "白天" + WeatherSlots.windText(w[0], w[1]) + "、无降水，适合喷药"
                        + rainAheadSuffix(b, day, zone);
            case FAIR:
                return dd + "白天" + WeatherSlots.windText(w[0], w[1]) + "，可以喷药，注意风力和降水变化"
                        + rainAheadSuffix(b, day, zone);
            default:
                return dd + "风力较大或有降水，不宜喷药，建议改期。";
        }
    }

    /** 「；12 日起有降雨，建议提前完成。」或「。」 */
    private static String rainAheadSuffix(WeatherBundle b, LocalDate day, ZoneId zone) {
        for (int i = 1; i <= RAIN_AHEAD_DAYS; i++) {
            LocalDate next = day.plusDays(i);
            DailyWeather d = WeatherSlots.dailyFor(b, next, zone);
            if (d == null) continue;
            if (d.precip > 0 || WeatherSlots.isRain(d.iconDay, d.textDay)) {
                return "；" + next.getDayOfMonth() + " 日起有降雨，建议提前完成。";
            }
            if (WeatherSlots.isSnow(d.iconDay, d.textDay)) {
                return "；" + next.getDayOfMonth() + " 日起有降雪，建议提前完成。";
            }
        }
        return "。";
    }

    // ———————————————————————— 喷药时段 ————————————————————————

    /** 单个小时的喷药等级（表 8-3） */
    static Suit classify(Slot s) {
        boolean tempOk = s.temp >= SPRAY_TEMP_MIN && s.temp <= SPRAY_TEMP_MAX;
        if (tempOk && s.pop < SPRAY_POP_MAX && s.windHi <= SPRAY_WIND_MAX) return Suit.GOOD;
        if (tempOk && s.pop <= SPRAY_POP_FAIR_MAX && s.windHi <= SPRAY_WIND_FAIR_MAX) return Suit.FAIR;
        return Suit.BAD;
    }

    /** 只有逐日预报时：白天风力、降水、气温 */
    static Suit classifyDaily(DailyWeather d) {
        int[] w = WeatherSlots.windRange(d.windScaleDay);
        boolean tempOk = d.tempMax >= SPRAY_TEMP_MIN && d.tempMin <= SPRAY_TEMP_MAX;
        boolean wet = WeatherSlots.hasPrecip(d);
        if (tempOk && !wet && w[1] <= SPRAY_WIND_MAX) return Suit.GOOD;
        if (tempOk && !wet && w[1] <= SPRAY_WIND_FAIR_MAX) return Suit.FAIR;
        return Suit.BAD;
    }

    static boolean isWorkHour(Slot s) {
        int h = s.hour();
        return h >= WORK_START_HOUR && h < WORK_END_HOUR;
    }

    /** 「未来 6 小时」中的作业小时；夜间（6 小时内没有作业小时）取下一个作业时段的前 6 小时 */
    static List<Slot> sprayCandidates(List<Slot> slots, long now, ZoneId zone) {
        long hourStart = WeatherSlots.floorHour(now, zone);
        long end = hourStart + SPRAY_LOOKAHEAD_HOURS * WeatherSlots.HOUR;
        List<Slot> out = new ArrayList<>();
        for (Slot s : slots) {
            if (s.start < end && isWorkHour(s)) out.add(s);
        }
        if (!out.isEmpty()) return out;
        LocalDate date = null;
        for (Slot s : slots) {
            if (!isWorkHour(s)) {
                if (date != null) break;
                continue;
            }
            if (date == null) date = s.date();
            if (!s.date().equals(date) || out.size() >= SPRAY_LOOKAHEAD_HOURS) break;
            out.add(s);
        }
        return out;
    }

    /** 推荐喷药时段：候选小时中第一个适宜的小时开始、连续适宜的作业小时（可超出 6 小时）；没有返回 null */
    @Nullable
    static Window sprayWindow(List<Slot> slots, long now, ZoneId zone) {
        List<Slot> cand = sprayCandidates(slots, now, zone);
        Slot first = null;
        for (Slot s : cand) {
            if (classify(s) == Suit.GOOD) {
                first = s;
                break;
            }
        }
        if (first == null) return null;
        Window w = new Window();
        boolean started = false;
        for (Slot s : slots) {
            if (!started) {
                if (s == first) {
                    started = true;
                    w.slots.add(s);
                }
                continue;
            }
            Slot prev = w.slots.get(w.slots.size() - 1);
            if (s.start != prev.start + WeatherSlots.HOUR || !isWorkHour(s) || classify(s) != Suit.GOOD) break;
            w.slots.add(s);
        }
        return w;
    }

    /** 最长的连续时段（等级不低于 min；一样长取最早），没有返回 null */
    @Nullable
    static Window longestRun(List<Slot> slots, Suit min) {
        Window best = null;
        Window cur = null;
        for (Slot s : slots) {
            boolean ok = classify(s).ordinal() <= min.ordinal();
            boolean contiguous = cur != null
                    && s.start == cur.slots.get(cur.slots.size() - 1).start + WeatherSlots.HOUR;
            if (!ok) {
                cur = null;
                continue;
            }
            if (cur == null || !contiguous) {
                cur = new Window();
            }
            cur.slots.add(s);
            if (best == null || cur.slots.size() > best.slots.size()) best = cur;
        }
        return best;
    }

    /** 「15:00–18:00」，不是今天时加「明天 」或「12日 」 */
    static String windowText(Window w, long now, ZoneId zone) {
        LocalDate today = WeatherSlots.localDate(now, zone);
        LocalDate date = WeatherSlots.localDate(w.start(), zone);
        String prefix = "";
        if (date.equals(today.plusDays(1))) prefix = "明天 ";
        else if (!date.equals(today)) prefix = date.getDayOfMonth() + "日 ";
        return prefix + range(w, zone);
    }

    /** 「7:00–10:00」 */
    static String range(Window w, ZoneId zone) {
        return hm(w.start(), zone) + "–" + hm(w.end(), zone);
    }

    private static String hm(long millis, ZoneId zone) {
        LocalTime t = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime();
        return t.getHour() + ":" + (t.getMinute() < 10 ? "0" : "") + t.getMinute();
    }

    // ———————————————————————— 降水 / 湿度 ————————————————————————

    /** 未来 24 小时降水量：逐小时预报求和；没有逐小时预报时取今天的逐日降水量 */
    static double rain24(WeatherBundle b, List<Slot> slots, long now, ZoneId zone) {
        long end = WeatherSlots.floorHour(now, zone) + 24 * WeatherSlots.HOUR;
        double sum = 0;
        int count = 0;
        for (Slot s : slots) {
            if (s.synthetic || s.start >= end) continue;
            sum += Math.max(0, s.precip);
            count++;
        }
        if (count > 0) return sum;
        DailyWeather d = WeatherSlots.dailyFor(b, WeatherSlots.localDate(now, zone), zone);
        return d == null ? 0 : d.precip;
    }

    static boolean moderateRain24(WeatherBundle b, List<Slot> slots, long now, ZoneId zone) {
        long end = WeatherSlots.floorHour(now, zone) + 24 * WeatherSlots.HOUR;
        int count = 0;
        for (Slot s : slots) {
            if (s.start >= end) continue;
            if (WeatherSlots.isModerateRainIcon(s.icon)) return true;
            if (!s.synthetic) {
                count++;
                if (s.precip >= MODERATE_RAIN_HOUR_MM) return true;
            }
        }
        if (count > 0) return rain24(b, slots, now, zone) >= MODERATE_RAIN_DAY_MM;
        DailyWeather d = WeatherSlots.dailyFor(b, WeatherSlots.localDate(now, zone), zone);
        return d != null && (d.precip >= MODERATE_RAIN_DAY_MM || WeatherSlots.isModerateRainIcon(d.iconDay));
    }

    /** 实时湿度；没有实时数据时取今天的逐日湿度；都没有返回 -1 */
    static int currentHumidity(WeatherBundle b, long now, ZoneId zone) {
        if (b.now != null) return b.now.humidity;
        DailyWeather d = WeatherSlots.dailyFor(b, WeatherSlots.localDate(now, zone), zone);
        return d == null ? -1 : d.humidity;
    }

    private static String levelOf(Suit s) {
        switch (s) {
            case GOOD:
                return LEVEL_SUITABLE;
            case FAIR:
                return LEVEL_FAIR;
            default:
                return LEVEL_UNSUITABLE;
        }
    }

    private static FarmIndex index(String name, String level, @Nullable String detail, int icon, boolean warning) {
        FarmIndex i = new FarmIndex();
        i.name = name;
        i.level = level;
        i.detail = detail;
        i.icon = icon;
        i.warning = warning;
        return i;
    }
}
