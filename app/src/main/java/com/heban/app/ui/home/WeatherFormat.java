package com.heban.app.ui.home;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TypefaceSpan;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.heban.app.R;
import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.FarmIndex;
import com.heban.app.data.model.HourlyWeather;
import com.heban.app.data.model.WeatherAlert;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.model.WeatherNow;
import com.heban.app.util.WeatherIcons;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

/** H01 / H02 天气文字、图标与颜色的格式化（文字取自 strings_home.xml）。 */
public final class WeatherFormat {
    private static final Locale CN = Locale.SIMPLIFIED_CHINESE;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm", CN);
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEE", CN);
    /** H02 最高温不超过该值的日期，温度条用 Tertiary（偏冷）色 */
    static final int COLD_MAX_TEMP = 15;
    /** H02 逐小时最多显示的条数（含「现在」） */
    static final int MAX_HOURS = 24;

    /** 当前时间来源（本地截图检查时可替换为固定时间） */
    @VisibleForTesting
    public static LongSupplier clock = System::currentTimeMillis;

    private WeatherFormat() {
    }

    public static long currentTimeMillis() {
        return clock.getAsLong();
    }

    private static ZonedDateTime at(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
    }

    // —— 温度与文字 ——

    @NonNull
    public static String temp(int t) {
        return t + "°";
    }

    /** 当前温度「24°」，没有数据时为空 */
    @NonNull
    public static String nowTemp(@NonNull WeatherStateUi ui) {
        WeatherBundle b = ui.bundle();
        return b == null ? "" : temp(b.now.temp);
    }

    /** 当前天气文字「多云转晴」 */
    @NonNull
    public static String nowText(@NonNull WeatherStateUi ui) {
        WeatherBundle b = ui.bundle();
        return b == null ? "" : nonNull(b.now.text);
    }

    @NonNull
    public static String tip(@NonNull WeatherStateUi ui) {
        return nonNull(ui.farmTip());
    }

    /** H01 预警栏：「降温预警 12日起最低 8°C，注意大棚保温」 */
    @NonNull
    public static CharSequence homeAlert(@NonNull WeatherStateUi ui) {
        WeatherAlert a = ui.alert();
        return a == null ? "" : alertText(a.title, a.shortText, " ");
    }

    /** H02 预警栏：「降温预警 · 12–14 日受冷空气影响……」 */
    @NonNull
    public static CharSequence detailAlert(@NonNull Context c, @NonNull WeatherStateUi ui) {
        WeatherAlert a = ui.alert();
        if (a == null) return "";
        String text = a.detailText != null && !a.detailText.isEmpty() ? a.detailText : a.shortText;
        return alertText(a.title, text, c.getString(R.string.weather_alert_separator));
    }

    @NonNull
    public static String updatedAt(@NonNull Context c, @NonNull WeatherStateUi ui) {
        WeatherBundle b = ui.bundle();
        return b == null ? "" : updatedAt(c, b.fetchedAt);
    }

    @NonNull
    public static String offlineText(@NonNull Context c, @NonNull WeatherStateUi ui) {
        return offlineText(c, ui.bundle());
    }

    /** 今天的逐日预报（找不到当天时取第一条） */
    @Nullable
    static DailyWeather today(@Nullable WeatherBundle b, long now) {
        if (b == null || b.daily == null || b.daily.isEmpty()) return null;
        LocalDate d = at(now).toLocalDate();
        for (DailyWeather w : b.daily) {
            if (at(w.date).toLocalDate().equals(d)) return w;
        }
        return b.daily.get(0);
    }

    /** H01 天气卡第二行：「18° / 26° · 湿度 68% · 东南风 2 级」 */
    @NonNull
    public static String homeDetail(@NonNull Context c, @Nullable WeatherBundle b) {
        if (b == null || b.now == null) return "";
        WeatherNow n = b.now;
        DailyWeather d = today(b, currentTimeMillis());
        String wind = nonNull(n.windDir);
        String scale = nonNull(n.windScale);
        return d != null
                ? c.getString(R.string.home_weather_detail, d.tempMin, d.tempMax, n.humidity, wind, scale)
                : c.getString(R.string.home_weather_detail_no_range, n.humidity, wind, scale);
    }

    /** H02 当前天气：「多云转晴 · 18° / 26° · 体感 25°」 */
    @NonNull
    public static String currentDetail(@NonNull Context c, @Nullable WeatherBundle b) {
        if (b == null || b.now == null) return "";
        WeatherNow n = b.now;
        DailyWeather d = today(b, currentTimeMillis());
        return d != null
                ? c.getString(R.string.weather_current_detail, nonNull(n.text), d.tempMin, d.tempMax, n.feelsLike)
                : c.getString(R.string.weather_current_detail_no_range, nonNull(n.text), n.feelsLike);
    }

    /** H01 顶部：「10月10日 周六 · 南京 江宁」（没有地点时只显示日期） */
    @NonNull
    public static String headerDate(@NonNull Context c, @Nullable Long now, @Nullable String location) {
        long t = now != null ? now : currentTimeMillis();
        String date = at(t).format(DateTimeFormatter.ofPattern(c.getString(R.string.home_date_pattern), CN));
        return location == null || location.isEmpty() ? date : c.getString(R.string.home_date_location, date, location);
    }

    /** 「降温预警 12日起最低 8°C，注意大棚保温」：标题用 Medium 字重 */
    @NonNull
    public static CharSequence alertText(@Nullable String title, @Nullable String text, @NonNull String separator) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append(title, new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL)),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (text != null && !text.isEmpty()) sb.append(separator);
        }
        if (text != null) sb.append(text);
        return sb;
    }

    /** 「15:02 更新」 */
    @NonNull
    public static String updatedAt(@NonNull Context c, long fetchedAt) {
        return fetchedAt <= 0 ? "" : c.getString(R.string.weather_updated, timeOrDate(c, fetchedAt));
    }

    /** 离线提示栏：「当前离线，显示 15:02 更新的天气」 */
    @NonNull
    public static String offlineText(@NonNull Context c, @Nullable WeatherBundle b) {
        if (b == null || b.fetchedAt <= 0) return c.getString(R.string.weather_offline_plain);
        return c.getString(R.string.weather_offline, timeOrDate(c, b.fetchedAt));
    }

    /** 今天只显示时分，否则加上日期 */
    private static String timeOrDate(Context c, long millis) {
        ZonedDateTime t = at(millis);
        String hm = t.format(HH_MM);
        if (t.toLocalDate().equals(at(currentTimeMillis()).toLocalDate())) return hm;
        return c.getString(R.string.weather_date_time, t.getMonthValue(), t.getDayOfMonth(), hm);
    }

    /** H02 未来 7 天的日期：「今天」「周日 11日」 */
    @NonNull
    public static String dayLabel(@NonNull Context c, long date, long now) {
        LocalDate d = at(date).toLocalDate();
        if (d.equals(at(now).toLocalDate())) return c.getString(R.string.weather_today);
        return c.getString(R.string.weather_day, d.format(WEEKDAY), d.getDayOfMonth());
    }

    static boolean isToday(long date, long now) {
        return at(date).toLocalDate().equals(at(now).toLocalDate());
    }

    @NonNull
    public static String hourLabel(@NonNull Context c, long time) {
        return c.getString(R.string.weather_hour, at(time).getHour());
    }

    // —— 逐小时 ——

    /** H02 逐小时一格：time 为 0 表示「现在」 */
    static final class Hour {
        final long time;
        final int temp;
        @DrawableRes final int icon;

        Hour(long time, int temp, @DrawableRes int icon) {
            this.time = time;
            this.temp = temp;
            this.icon = icon;
        }

        boolean isNow() {
            return time == 0;
        }
    }

    /** 「现在」（实时天气）+ 当前小时之后的逐小时预报，最多 MAX_HOURS 格 */
    @NonNull
    static List<Hour> hours(@Nullable WeatherBundle b, long now) {
        List<Hour> out = new ArrayList<>();
        if (b == null || b.now == null) return out;
        out.add(new Hour(0, b.now.temp, WeatherIcons.forCode(b.now.icon)));
        long currentHour = at(now).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli();
        if (b.hourly != null) {
            for (HourlyWeather h : b.hourly) {
                if (out.size() >= MAX_HOURS) break;
                if (h.time > currentHour) out.add(new Hour(h.time, h.temp, WeatherIcons.forCode(h.icon)));
            }
        }
        return out;
    }

    // —— 未来 7 天温度条 ——

    /** 温度条位置：以这几天的最低、最高温为两端，返回 [left, width]（0–1） */
    @NonNull
    static float[] range(int min, int max, int weekMin, int weekMax) {
        float span = weekMax - weekMin;
        if (span <= 0) return new float[]{0f, 1f};
        float left = (min - weekMin) / span;
        float width = (max - min) / span;
        left = Math.max(0f, Math.min(1f, left));
        width = Math.max(0f, Math.min(1f - left, width));
        return new float[]{left, width};
    }

    static boolean isCold(@NonNull DailyWeather d) {
        return d.tempMax <= COLD_MAX_TEMP;
    }

    // —— 图标与颜色 ——

    /** 天气卡大图标用实心样式（原型 .ms.f） */
    @DrawableRes
    public static int filledIcon(@DrawableRes int outlined) {
        if (outlined == R.drawable.ic_partly_cloudy_day) return R.drawable.ic_partly_cloudy_day_fill;
        if (outlined == R.drawable.ic_nights_stay) return R.drawable.ic_nights_stay_fill;
        if (outlined == R.drawable.ic_sunny) return R.drawable.weather_ic_sunny_fill;
        if (outlined == R.drawable.ic_cloud) return R.drawable.weather_ic_cloud_fill;
        if (outlined == R.drawable.ic_thunderstorm) return R.drawable.weather_ic_thunderstorm_fill;
        if (outlined == R.drawable.ic_rainy) return R.drawable.weather_ic_rainy_fill;
        if (outlined == R.drawable.ic_rainy_light) return R.drawable.ic_rainy_light_fill;
        if (outlined == R.drawable.ic_rainy_heavy) return R.drawable.ic_rainy_heavy_fill;
        if (outlined == R.drawable.ic_weather_mix) return R.drawable.ic_weather_mix_fill;
        if (outlined == R.drawable.ic_weather_snowy) return R.drawable.ic_weather_snowy_fill;
        if (outlined == R.drawable.ic_foggy) return R.drawable.ic_foggy_fill;
        if (outlined == R.drawable.ic_ac_unit) return R.drawable.weather_ic_ac_unit_fill;
        return outlined;
    }

    @DrawableRes
    public static int nowIcon(@Nullable WeatherBundle b) {
        return filledIcon(WeatherIcons.forCode(b == null || b.now == null ? null : b.now.icon));
    }

    /** 天气卡农事建议的图标：湿度相关用 humidity_high，其余用 spa */
    @DrawableRes
    public static int tipIcon(@Nullable String tip) {
        return tip != null && tip.contains("湿") ? R.drawable.ic_humidity_high : R.drawable.ic_spa;
    }

    /** H02 未来 7 天图标颜色：晴 Warning、多云 Primary、雨雪 Tertiary、阴雾 On Surface Variant */
    @AttrRes
    static int dayIconColor(@DrawableRes int icon) {
        if (icon == R.drawable.ic_sunny) return R.attr.colorWarning;
        if (icon == R.drawable.ic_partly_cloudy_day) return androidx.appcompat.R.attr.colorPrimary;
        if (icon == R.drawable.ic_rainy || icon == R.drawable.ic_rainy_light || icon == R.drawable.ic_rainy_heavy
                || icon == R.drawable.ic_thunderstorm || icon == R.drawable.ic_weather_mix
                || icon == R.drawable.ic_weather_snowy || icon == R.drawable.ic_ac_unit) {
            return com.google.android.material.R.attr.colorTertiary;
        }
        return com.google.android.material.R.attr.colorOnSurfaceVariant;
    }

    /** H02 农事指数图标颜色（原型：喷药 / 施肥 Primary，灌溉 / 通风 Tertiary，晾晒 Warning，霜冻等提醒 Error） */
    @AttrRes
    static int indexIconColor(@NonNull FarmIndex index) {
        if (index.warning) return androidx.appcompat.R.attr.colorError;
        String n = nonNull(index.name);
        if (n.contains("灌溉") || n.contains("通风")) return com.google.android.material.R.attr.colorTertiary;
        if (n.contains("晾晒")) return R.attr.colorWarning;
        if (n.contains("霜冻")) return androidx.appcompat.R.attr.colorError;
        return androidx.appcompat.R.attr.colorPrimary;
    }

    /** H02 农事指数等级颜色：提醒类 Error，「适宜」Primary，其余正文色 */
    @AttrRes
    static int indexLevelColor(@NonNull FarmIndex index) {
        if (index.warning) return androidx.appcompat.R.attr.colorError;
        if ("适宜".equals(index.level)) return androidx.appcompat.R.attr.colorPrimary;
        return com.google.android.material.R.attr.colorOnSurface;
    }

    private static String nonNull(@Nullable String s) {
        return s == null ? "" : s;
    }
}
