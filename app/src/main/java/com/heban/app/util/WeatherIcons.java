package com.heban.app.util;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.heban.app.R;

/** 和风天气图标代码 → Material Symbols 图标（与原型一致：晴 sunny、多云 partly_cloudy_day、夜间 nights_stay ……）。 */
public final class WeatherIcons {
    private WeatherIcons() {
    }

    @DrawableRes
    public static int forCode(@Nullable String code) {
        int c;
        try {
            c = Integer.parseInt(code == null ? "" : code.trim());
        } catch (NumberFormatException e) {
            return R.drawable.ic_cloud;
        }
        if (c == 100 || c == 900) return R.drawable.ic_sunny;
        if (c >= 101 && c <= 103) return R.drawable.ic_partly_cloudy_day;
        if (c >= 150 && c <= 153) return R.drawable.ic_nights_stay;
        if (c == 104) return R.drawable.ic_cloud;
        if (c >= 302 && c <= 304) return R.drawable.ic_thunderstorm;
        if (c == 305 || c == 309 || c == 314) return R.drawable.ic_rainy_light;
        if (c == 307 || c == 308 || (c >= 310 && c <= 312) || (c >= 316 && c <= 318)) return R.drawable.ic_rainy_heavy;
        if (c == 313) return R.drawable.ic_weather_mix;
        if (c >= 300 && c <= 399) return R.drawable.ic_rainy;
        if ((c >= 404 && c <= 406) || c == 456 || c == 457) return R.drawable.ic_weather_mix;
        if (c >= 400 && c <= 499) return R.drawable.ic_weather_snowy;
        if (c >= 500 && c <= 599) return R.drawable.ic_foggy;
        if (c == 901) return R.drawable.ic_ac_unit;
        return R.drawable.ic_cloud;
    }
}
