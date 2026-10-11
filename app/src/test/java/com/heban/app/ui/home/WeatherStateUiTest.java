package com.heban.app.ui.home;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.R;
import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.FarmIndex;
import com.heban.app.data.model.HourlyWeather;
import com.heban.app.data.model.WeatherAlert;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.model.WeatherNow;
import com.heban.app.data.model.WeatherState;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.weather.QWeatherErrors;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class WeatherStateUiTest {

    private static long at(int hour, int minute) {
        return LocalDateTime.of(2026, 10, 10, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static WeatherBundle bundle() {
        WeatherBundle b = new WeatherBundle();
        b.locationName = "南京 江宁";
        b.now = new WeatherNow();
        b.now.temp = 24;
        b.now.icon = "101";
        b.now.text = "多云";
        return b;
    }

    private static WeatherState error(AppError.Type type, String msg) {
        WeatherState s = new WeatherState();
        s.error = new AppError(type, 0, msg);
        return s;
    }

    // ---- 状态 ----

    @Test
    public void loading_showsSkeleton() {
        WeatherStateUi ui = new WeatherStateUi(null);
        assertTrue(ui.showSkeleton());
        assertFalse(ui.showError());
        assertFalse(ui.hasData());
        assertNull(ui.locationName());
    }

    @Test
    public void data_withOfflineAndAlert() {
        WeatherState s = new WeatherState();
        s.bundle = bundle();
        s.offline = true;
        s.alert = new WeatherAlert();
        s.farmTip = "";
        WeatherStateUi ui = new WeatherStateUi(s);
        assertTrue(ui.hasData());
        assertTrue(ui.showOffline());
        assertTrue(ui.showAlert());
        assertFalse(ui.showSkeleton());
        assertFalse(ui.showError());
        assertEquals("南京 江宁", ui.locationName());
        assertNull(ui.farmTip());
        assertFalse(ui.hasIndices());
        assertFalse(ui.hasDaily());
        assertEquals("24°", WeatherFormat.nowTemp(ui));
        assertEquals("多云", WeatherFormat.nowText(ui));
    }

    @Test
    public void bundleWithoutNow_isNotData() {
        WeatherState s = new WeatherState();
        s.bundle = new WeatherBundle();
        assertFalse(new WeatherStateUi(s).hasData());
        assertEquals("", WeatherFormat.nowTemp(new WeatherStateUi(s)));
    }

    @Test
    public void missingKey_showsMessageOnly() {
        WeatherStateUi ui = new WeatherStateUi(error(AppError.Type.BUSINESS, "未配置和风天气 API Key"));
        assertTrue(ui.showError());
        assertFalse(ui.needsCityPick());
        assertFalse(ui.canRetry());
        assertEquals("未配置和风天气 API Key", ui.errorMessage());
    }

    @Test
    public void missingLocation_offersCityPick() {
        WeatherStateUi ui = new WeatherStateUi(error(AppError.Type.BUSINESS, QWeatherErrors.MSG_NO_LOCATION));
        assertTrue(ui.needsCityPick());
        assertFalse(ui.canRetry());
    }

    @Test
    public void networkError_offersRetry() {
        WeatherStateUi ui = new WeatherStateUi(error(AppError.Type.NETWORK, "网络不太好，检查后再试一次"));
        assertTrue(ui.canRetry());
        assertFalse(ui.needsCityPick());
    }

    @Test
    public void errorWithCache_showsDataNotError() {
        WeatherState s = error(AppError.Type.NETWORK, "网络不太好");
        s.bundle = bundle();
        s.offline = true;
        WeatherStateUi ui = new WeatherStateUi(s);
        assertFalse(ui.showError());
        assertFalse(ui.canRetry());
        assertTrue(ui.showOffline());
    }

    @Test
    public void failed_convertsThrowable() {
        WeatherState s = WeatherStateUi.failed(new java.io.IOException("x"));
        assertEquals(AppError.Type.NETWORK, s.error.type);
    }

    // ---- 格式化（不依赖 Context 的部分） ----

    @Test
    public void range_usesWeekMinMax() {
        // 原型：今天 18–26（一周 7–26）→ left 58%，width 42%
        float[] r = WeatherFormat.range(18, 26, 7, 26);
        assertEquals(0.58f, r[0], 0.01f);
        assertEquals(0.42f, r[1], 0.01f);
        float[] r2 = WeatherFormat.range(17, 23, 7, 26);
        assertEquals(0.53f, r2[0], 0.01f);
        assertEquals(0.32f, r2[1], 0.01f);
        assertArrayEquals(new float[]{0f, 1f}, WeatherFormat.range(5, 5, 5, 5), 0f);
    }

    @Test
    public void cold_whenMaxAtMost15() {
        DailyWeather d = new DailyWeather();
        d.tempMax = 15;
        assertTrue(WeatherFormat.isCold(d));
        d.tempMax = 17;
        assertFalse(WeatherFormat.isCold(d));
    }

    @Test
    public void hours_startWithNowAndSkipPast() {
        WeatherBundle b = bundle();
        for (int h = 14; h <= 20; h++) {
            HourlyWeather w = new HourlyWeather();
            w.time = at(h, 0);
            w.temp = h;
            w.icon = "100";
            b.hourly.add(w);
        }
        List<WeatherFormat.Hour> hours = WeatherFormat.hours(b, at(15, 2));
        assertTrue(hours.get(0).isNow());
        assertEquals(24, hours.get(0).temp);
        assertEquals(R.drawable.ic_partly_cloudy_day, hours.get(0).icon);
        assertEquals(6, hours.size());                    // 现在 + 16…20 时
        assertEquals(at(16, 0), hours.get(1).time);
        assertEquals(R.drawable.ic_sunny, hours.get(1).icon);
        assertTrue(WeatherFormat.hours(null, at(15, 2)).isEmpty());
    }

    @Test
    public void today_findsCurrentDay() {
        WeatherBundle b = bundle();
        DailyWeather y = new DailyWeather();
        y.date = at(0, 0) - 86_400_000L;
        y.tempMin = 1;
        DailyWeather t = new DailyWeather();
        t.date = at(0, 0);
        t.tempMin = 18;
        b.daily.add(y);
        b.daily.add(t);
        assertEquals(18, WeatherFormat.today(b, at(15, 0)).tempMin);
        assertTrue(WeatherFormat.isToday(t.date, at(23, 59)));
        assertFalse(WeatherFormat.isToday(y.date, at(0, 1)));
    }

    @Test
    public void icons_filledAndTip() {
        assertEquals(R.drawable.ic_partly_cloudy_day_fill, WeatherFormat.filledIcon(R.drawable.ic_partly_cloudy_day));
        assertEquals(R.drawable.ic_nights_stay_fill, WeatherFormat.filledIcon(R.drawable.ic_nights_stay));
        assertEquals(R.drawable.weather_ic_sunny_fill, WeatherFormat.filledIcon(R.drawable.ic_sunny));
        assertEquals(R.drawable.ic_humidity_high, WeatherFormat.tipIcon("夜间湿度高，大棚注意通风排湿"));
        assertEquals(R.drawable.ic_spa, WeatherFormat.tipIcon("15:00–18:00 风力小，适宜喷药"));
        assertEquals(R.drawable.ic_spa, WeatherFormat.tipIcon(null));
    }

    @Test
    public void indexColors() {
        FarmIndex spray = new FarmIndex();
        spray.name = "喷药";
        spray.level = "适宜";
        assertEquals(androidx.appcompat.R.attr.colorPrimary, WeatherFormat.indexIconColor(spray));
        assertEquals(androidx.appcompat.R.attr.colorPrimary, WeatherFormat.indexLevelColor(spray));
        FarmIndex water = new FarmIndex();
        water.name = "灌溉";
        water.level = "暂不需要";
        assertEquals(com.google.android.material.R.attr.colorTertiary, WeatherFormat.indexIconColor(water));
        assertEquals(com.google.android.material.R.attr.colorOnSurface, WeatherFormat.indexLevelColor(water));
        FarmIndex frost = new FarmIndex();
        frost.name = "霜冻";
        frost.level = "12日起注意";
        frost.warning = true;
        assertEquals(androidx.appcompat.R.attr.colorError, WeatherFormat.indexIconColor(frost));
        assertEquals(androidx.appcompat.R.attr.colorError, WeatherFormat.indexLevelColor(frost));
    }
}
