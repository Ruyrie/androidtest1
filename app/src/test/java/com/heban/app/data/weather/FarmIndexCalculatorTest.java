package com.heban.app.data.weather;

import static com.heban.app.data.weather.WeatherFixtures.ZONE;
import static com.heban.app.data.weather.WeatherFixtures.at;
import static com.heban.app.data.weather.WeatherFixtures.daily;
import static com.heban.app.data.weather.WeatherFixtures.hourly;
import static com.heban.app.data.weather.WeatherFixtures.now;
import static com.heban.app.data.weather.WeatherFixtures.prototype;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.R;
import com.heban.app.data.model.FarmIndex;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.HourlyWeather;
import com.heban.app.data.model.WeatherBundle;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FarmIndexCalculatorTest {

    private static FarmIndex find(List<FarmIndex> list, String name) {
        for (FarmIndex i : list) if (name.equals(i.name)) return i;
        throw new AssertionError("missing " + name);
    }

    private static List<FarmIndex> indices(WeatherBundle b, long now) {
        return FarmIndexCalculator.indices(b, now, ZONE);
    }

    // —— 原型数据 ——

    @Test
    public void prototype_producesPrototypeIndexTexts() {
        List<FarmIndex> list = indices(prototype(), now());
        assertEquals(6, list.size());
        String[] names = new String[6];
        for (int i = 0; i < 6; i++) names[i] = list.get(i).name;
        assertArrayEquals(new String[]{"喷药", "灌溉", "施肥", "晾晒", "霜冻", "通风"}, names);

        FarmIndex spray = find(list, "喷药");
        assertEquals("适宜", spray.level);
        assertEquals("15:00–18:00", spray.detail);
        assertEquals(R.drawable.ic_sanitizer, spray.icon);
        assertEquals("暂不需要", find(list, "灌溉").level);
        assertEquals("适宜", find(list, "施肥").level);
        assertEquals("较适宜", find(list, "晾晒").level);
        assertEquals("上午为宜", find(list, "通风").level);
        assertEquals(R.drawable.ic_air, find(list, "通风").icon);
    }

    @Test
    public void prototype_farmTip() {
        assertEquals("15:00–18:00 风力小，适宜喷药", FarmIndexCalculator.farmTip(prototype(), now(), ZONE));
    }

    @Test
    public void prototype_farmTaskTip_spray() {
        assertEquals("11 日 7:00–10:00 风力 1 级、无降水，适合喷药；12 日起有降雨，建议提前完成。",
                FarmIndexCalculator.farmTaskTip(prototype(), FarmType.SPRAY, LocalDate.of(2026, 10, 11),
                        now(), ZONE));
    }

    // —— 霜冻（表 8-3：未来 7 天最低温 ≤ 2°C 提示首次日期） ——

    @Test
    public void frost_prototypeTemperatures_noRisk() {
        // 原型 7 天最低温 7–18°C，按 ≤ 2°C 的规则不提示（原型图中的「12日起注意」与规则不一致，见报告）
        FarmIndex frost = find(indices(prototype(), now()), "霜冻");
        assertEquals("无风险", frost.level);
        assertFalse(frost.warning);
    }

    @Test
    public void frost_firstDayAtOrBelow2_isReported() {
        WeatherBundle b = prototype();
        daily(b, 12).tempMin = 2;
        daily(b, 14).tempMin = -1;
        FarmIndex frost = find(indices(b, now()), "霜冻");
        assertEquals("12日起注意", frost.level);
        assertEquals("最低 2°C", frost.detail);
        assertTrue(frost.warning);
        assertEquals(R.drawable.ic_severe_cold, frost.icon);
    }

    @Test
    public void frost_3DegreesIsNotFrost_lastForecastDayCounts() {
        WeatherBundle b = prototype();
        daily(b, 12).tempMin = 3;
        assertEquals("无风险", find(indices(b, now()), "霜冻").level);
        daily(b, 16).tempMin = 0;
        assertEquals("16日起注意", find(indices(b, now()), "霜冻").level);
    }

    // —— 喷药时段 ——

    @Test
    public void spray_wind4_isFair_noWindow() {
        WeatherBundle b = prototype();
        b.now.windScale = "4";
        hourly(b, 10, 16).windScale = "3-4";
        hourly(b, 10, 17).windScale = "4";
        FarmIndex spray = find(indices(b, now()), "喷药");
        assertEquals("较适宜", spray.level);
        assertNull(spray.detail);
    }

    @Test
    public void spray_pop30to60_isFair_above60_isUnsuitable() {
        WeatherBundle b = prototype();
        for (int h = 16; h <= 18; h++) hourly(b, 10, h).pop = 45;
        b.now.precip = 0;
        // 当前小时借用下一小时的降水概率（45%）
        assertEquals("较适宜", find(indices(b, now()), "喷药").level);
        for (int h = 16; h <= 18; h++) hourly(b, 10, h).pop = 70;
        assertEquals("不宜", find(indices(b, now()), "喷药").level);
    }

    @Test
    public void spray_temperatureOutside10to30_isUnsuitable() {
        WeatherBundle b = prototype();
        b.now.temp = 31;
        for (int h = 16; h <= 18; h++) hourly(b, 10, h).temp = 32;
        assertEquals("不宜", find(indices(b, now()), "喷药").level);
        b.now.temp = 9;
        for (int h = 16; h <= 18; h++) hourly(b, 10, h).temp = 9;
        assertEquals("不宜", find(indices(b, now()), "喷药").level);
    }

    @Test
    public void spray_windowStartsLaterWhenFirstHoursAreNotSuitable() {
        WeatherBundle b = prototype();
        b.now.windScale = "5";
        FarmIndex spray = find(indices(b, now()), "喷药");
        assertEquals("适宜", spray.level);
        assertEquals("16:00–18:00", spray.detail);
    }

    @Test
    public void spray_atNight_looksAtTomorrowsWorkHours() {
        WeatherBundle b = prototype();
        long night = at(10, 21, 30);
        FarmIndex spray = find(indices(b, night), "喷药");
        assertEquals("适宜", spray.level);
        assertEquals("明天 7:00–10:00", spray.detail);
        assertEquals("明天 7:00–10:00 风力小，适宜喷药", FarmIndexCalculator.farmTip(b, night, ZONE));
    }

    @Test
    public void spray_missingPop_usesPrecipitation() {
        WeatherBundle b = prototype();
        for (int h = 16; h <= 18; h++) {
            hourly(b, 10, h).pop = -1;
            hourly(b, 10, h).precip = 0.3;
        }
        b.now.precip = 0.2;
        assertEquals("不宜", find(indices(b, now()), "喷药").level);
    }

    @Test
    public void spray_noHourlyLeft_usesTodaysDailyForecast() {
        WeatherBundle b = prototype();
        b.hourly.clear();
        b.now = null;
        FarmIndex spray = find(indices(b, now()), "喷药");
        assertEquals("适宜", spray.level);   // 今天 多云、风力 1-2 级、无降水、18–26°C
        assertNull(spray.detail);
    }

    // —— 灌溉 / 施肥 / 晾晒 / 通风 ——

    @Test
    public void irrigate_dryAndNoRain_isSuitable_otherwiseDependsOnSoil() {
        WeatherBundle b = prototype();
        for (HourlyWeather h : b.hourly) h.precip = 0;
        b.now.humidity = 45;
        assertEquals("适宜", find(indices(b, now()), "灌溉").level);
        b.now.humidity = 55;
        assertEquals("视墒情", find(indices(b, now()), "灌溉").level);
        hourly(b, 11, 2).precip = 4.9;
        assertEquals("视墒情", find(indices(b, now()), "灌溉").level);
        hourly(b, 11, 3).precip = 0.1;
        FarmIndex i = find(indices(b, now()), "灌溉");
        assertEquals("暂不需要", i.level);
        assertEquals("未来 24 小时降水约 5 mm", i.detail);
    }

    @Test
    public void fertilize_moderateRainWithin24h_isUnsuitable() {
        WeatherBundle b = prototype();
        hourly(b, 11, 2).precip = 2.6;
        assertEquals("不宜", find(indices(b, now()), "施肥").level);

        b = prototype();
        hourly(b, 11, 2).icon = "306";   // 中雨
        assertEquals("不宜", find(indices(b, now()), "施肥").level);

        b = prototype();
        for (int h = 0; h <= 5; h++) hourly(b, 11, h).precip = 1.8;   // 10.8 mm
        assertEquals("不宜", find(indices(b, now()), "施肥").level);
    }

    @Test
    public void dry_rules() {
        WeatherBundle b = prototype();
        daily(b, 10).humidity = 65;
        assertEquals("适宜", find(indices(b, now()), "晾晒").level);
        daily(b, 10).humidity = 85;
        assertEquals("较适宜", find(indices(b, now()), "晾晒").level);
        daily(b, 10).humidity = 86;
        assertEquals("不宜", find(indices(b, now()), "晾晒").level);
        daily(b, 10).humidity = 50;
        daily(b, 10).iconDay = "305";
        daily(b, 10).textDay = "小雨";
        assertEquals("不宜", find(indices(b, now()), "晾晒").level);
    }

    @Test
    public void vent_nightOnlyHumidity_remindsDehumidify_dryAir_isSuitable() {
        WeatherBundle b = prototype();
        hourly(b, 11, 7).humidity = 80;
        hourly(b, 11, 8).humidity = 80;
        hourly(b, 11, 6).humidity = 80;
        assertEquals("夜间排湿", find(indices(b, now()), "通风").level);
        for (HourlyWeather h : b.hourly) h.humidity = 60;
        assertEquals("适宜", find(indices(b, now()), "通风").level);
        b.now.humidity = 90;
        assertEquals("上午为宜", find(indices(b, now()), "通风").level);
    }

    @Test
    public void indices_noData_isEmpty() {
        assertTrue(FarmIndexCalculator.indices(null, now(), ZONE).isEmpty());
        assertTrue(FarmIndexCalculator.indices(new WeatherBundle(), now(), ZONE).isEmpty());
        assertNull(FarmIndexCalculator.farmTip(null, now(), ZONE));
    }

    // —— H04 AI 提示 ——

    @Test
    public void farmTaskTip_emptyWhenNoDataPastOrBeyondForecast() {
        assertEquals("", FarmIndexCalculator.farmTaskTip(null, FarmType.SPRAY, LocalDate.of(2026, 10, 11), now(), ZONE));
        assertEquals("", FarmIndexCalculator.farmTaskTip(prototype(), FarmType.SPRAY, LocalDate.of(2026, 10, 9),
                now(), ZONE));
        assertEquals("", FarmIndexCalculator.farmTaskTip(prototype(), FarmType.SPRAY, LocalDate.of(2026, 10, 20),
                now(), ZONE));
        assertEquals("", FarmIndexCalculator.farmTaskTip(prototype(), null, LocalDate.of(2026, 10, 11), now(), ZONE));
    }

    @Test
    public void farmTaskTip_dailyOnly_forLaterDays() {
        WeatherBundle b = prototype();
        assertEquals("14 日白天风力 1–2 级、无降水，适合喷药。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.SPRAY, LocalDate.of(2026, 10, 14), now(), ZONE));
        assertEquals("12 日风力较大或有降水，不宜喷药，建议改期。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.SPRAY, LocalDate.of(2026, 10, 12), now(), ZONE));
        assertEquals("13 日白天风力 3–4 级，可以喷药，注意风力和降水变化。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.SPRAY, LocalDate.of(2026, 10, 13), now(), ZONE));
    }

    @Test
    public void farmTaskTip_today_usesRemainingHours() {
        // 11 日凌晨有小雨（逐日降水 6 mm），所以提示 11 日起有降雨
        assertEquals("10 日 15:00–18:00 风力 2 级、无降水，适合喷药；11 日起有降雨，建议提前完成。",
                FarmIndexCalculator.farmTaskTip(prototype(), FarmType.SPRAY, LocalDate.of(2026, 10, 10),
                        now(), ZONE));
    }

    @Test
    public void farmTaskTip_otherTypes() {
        WeatherBundle b = prototype();
        LocalDate d11 = LocalDate.of(2026, 10, 11);
        LocalDate d12 = LocalDate.of(2026, 10, 12);
        LocalDate d14 = LocalDate.of(2026, 10, 14);
        assertEquals("11 日无中雨以上降水，适合施肥。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.FERTILIZE, d11, now(), ZONE));
        assertEquals("12 日预计降水 8 mm，暂不需要灌溉。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.IRRIGATE, d12, now(), ZONE));
        assertEquals("14 日无明显降水，请视墒情灌溉。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.IRRIGATE, d14, now(), ZONE));
        assertEquals("12 日有降水，伤口不易愈合，建议改在晴天整枝。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.PRUNE, d12, now(), ZONE));
        assertEquals("14 日无降水，适合采收。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.HARVEST, d14, now(), ZONE));
        assertEquals("10 日无降水，适合采收；11 日起有降雨，建议提前完成。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.HARVEST, LocalDate.of(2026, 10, 10), now(), ZONE));
        assertEquals("14 日晴，8–17°C，无降水。",
                FarmIndexCalculator.farmTaskTip(b, FarmType.OTHER, d14, now(), ZONE));
    }

    @Test
    public void windRange_parsesQWeatherScales() {
        assertArrayEquals(new int[]{2, 2}, WeatherSlots.windRange("2"));
        assertArrayEquals(new int[]{3, 4}, WeatherSlots.windRange("3-4"));
        assertArrayEquals(new int[]{0, 0}, WeatherSlots.windRange(""));
        assertArrayEquals(new int[]{0, 0}, WeatherSlots.windRange(null));
    }

    @Test
    public void longestRun_prefersLongestThenEarliest() {
        WeatherBundle b = prototype();
        List<WeatherSlots.Slot> slots = new ArrayList<>();
        for (WeatherSlots.Slot s : WeatherSlots.slots(b, now(), ZONE)) {
            if (s.date().getDayOfMonth() == 11) slots.add(s);
        }
        FarmIndexCalculator.Window w = FarmIndexCalculator.longestRun(slots, FarmIndexCalculator.Suit.GOOD);
        assertEquals("7:00–10:00", FarmIndexCalculator.range(w, ZONE));
    }
}
