package com.heban.app.data.weather;

import static com.heban.app.data.weather.WeatherFixtures.ZONE;
import static com.heban.app.data.weather.WeatherFixtures.daily;
import static com.heban.app.data.weather.WeatherFixtures.now;
import static com.heban.app.data.weather.WeatherFixtures.prototype;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.WeatherBundle;

import org.junit.Test;

public class AttentionRulesTest {
    private static final long HOUR = 3_600_000L;

    private static CropEntity crop(String name, String mode) {
        CropEntity c = new CropEntity();
        c.id = 1;
        c.name = name;
        c.plantMode = mode;
        return c;
    }

    private static PlotEntity plot(String type, Integer humidity, long readingAt) {
        PlotEntity p = new PlotEntity();
        p.id = 2;
        p.type = type;
        p.indoorHumidity = humidity;
        p.readingAt = readingAt;
        return p;
    }

    @Test
    public void cucumberInGreenhouse_recentIndoorHumidity85_downyMildew() {
        WeatherBundle b = prototype();   // 室外今天湿度 72%
        PlotEntity p = plot(PlotEntity.TYPE_GREENHOUSE, 85, now() - 2 * HOUR);
        assertEquals("霜霉病风险",
                AttentionRules.evaluate(crop("黄瓜", CropEntity.MODE_GREENHOUSE), p, b, now(), ZONE));
        p.indoorHumidity = 84;
        assertNull(AttentionRules.evaluate(crop("黄瓜", CropEntity.MODE_GREENHOUSE), p, b, now(), ZONE));
    }

    @Test
    public void indoorReadingOlderThan24h_usesOutdoorForecast() {
        WeatherBundle b = prototype();
        PlotEntity p = plot(PlotEntity.TYPE_GREENHOUSE, 92, now() - 25 * HOUR);
        assertNull(AttentionRules.evaluate(crop("黄瓜", CropEntity.MODE_GREENHOUSE), p, b, now(), ZONE));
        daily(b, 10).humidity = 88;
        assertEquals("霜霉病风险",
                AttentionRules.evaluate(crop("黄瓜", CropEntity.MODE_GREENHOUSE), p, b, now(), ZONE));
    }

    @Test
    public void tomatoHighHumidity_grayMold() {
        WeatherBundle b = prototype();
        daily(b, 10).humidity = 90;
        assertEquals("灰霉病风险", AttentionRules.evaluate(crop("番茄", CropEntity.MODE_OPEN), null, b, now(), ZONE));
        assertEquals("灰霉病风险",
                AttentionRules.evaluate(crop("樱桃番茄", CropEntity.MODE_POT), null, b, now(), ZONE));
        assertNull(AttentionRules.evaluate(crop("辣椒", CropEntity.MODE_OPEN), null, b, now(), ZONE));
    }

    @Test
    public void openFieldFrostWithin3Days_frostRisk_greenhouseIgnoresFrost() {
        WeatherBundle b = prototype();
        daily(b, 12).tempMin = 2;
        assertEquals("霜冻风险", AttentionRules.evaluate(crop("冬小麦", CropEntity.MODE_OPEN), null, b, now(), ZONE));
        assertNull(AttentionRules.evaluate(crop("冬小麦", CropEntity.MODE_GREENHOUSE), null, b, now(), ZONE));
        assertNull(AttentionRules.evaluate(crop("冬小麦", CropEntity.MODE_POT), null, b, now(), ZONE));
        // 露天地块中的作物（种植方式未写露天）也按露天判断
        CropEntity inOpenPlot = crop("白菜", CropEntity.MODE_GREENHOUSE);
        inOpenPlot.plantMode = "";
        assertEquals("霜冻风险", AttentionRules.evaluate(inOpenPlot, plot(PlotEntity.TYPE_OPEN, null, 0), b,
                now(), ZONE));
    }

    @Test
    public void frostOnDay4_isNotWithin3Days() {
        WeatherBundle b = prototype();
        daily(b, 13).tempMin = 0;
        assertNull(AttentionRules.evaluate(crop("冬小麦", CropEntity.MODE_OPEN), null, b, now(), ZONE));
    }

    @Test
    public void frostTakesPriorityOverDisease() {
        WeatherBundle b = prototype();
        daily(b, 10).humidity = 90;
        daily(b, 11).tempMin = 1;
        assertEquals("霜冻风险", AttentionRules.evaluate(crop("黄瓜", CropEntity.MODE_OPEN), null, b, now(), ZONE));
    }

    @Test
    public void noWeather_null() {
        assertNull(AttentionRules.evaluate(crop("黄瓜", CropEntity.MODE_OPEN), null, null, now(), ZONE));
    }
}
