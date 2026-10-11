package com.heban.app.data.weather;

import static com.heban.app.data.weather.WeatherFixtures.ZONE;
import static com.heban.app.data.weather.WeatherFixtures.at;
import static com.heban.app.data.weather.WeatherFixtures.daily;
import static com.heban.app.data.weather.WeatherFixtures.now;
import static com.heban.app.data.weather.WeatherFixtures.prototype;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.heban.app.data.model.WeatherAlert;
import com.heban.app.data.model.WeatherBundle;

import org.junit.Test;

public class WeatherAlertRulesTest {

    private static WeatherBundle flat(int... mins) {
        WeatherBundle b = new WeatherBundle();
        for (int i = 0; i < mins.length; i++) {
            b.daily.add(WeatherFixtures.day(10 + i, mins[i] + 8, mins[i], "101", "多云", 0, 60, "1-2"));
        }
        return b;
    }

    @Test
    public void prototype_coolingAlertTexts() {
        WeatherAlert a = WeatherAlertRules.evaluate(prototype(), now(), ZONE);
        assertNotNull(a);
        assertEquals("降温预警", a.title);
        assertEquals("12日起最低 8°C，注意大棚保温", a.shortText);
        assertEquals("12–14 日受冷空气影响，最低 7°C，大棚夜间注意加盖保温被", a.detailText);
    }

    @Test
    public void dropOf8_triggers() {
        WeatherAlert a = WeatherAlertRules.evaluate(flat(20, 19, 12, 18, 19), now(), ZONE);
        assertNotNull(a);
        assertEquals("12日起最低 12°C，注意大棚保温", a.shortText);
        assertEquals("12 日受冷空气影响，最低 12°C，大棚夜间注意加盖保温被", a.detailText);
    }

    @Test
    public void dropOf7_above8_doesNotTrigger() {
        assertNull(WeatherAlertRules.evaluate(flat(20, 19, 13, 13, 14), now(), ZONE));
    }

    @Test
    public void lowTempAtMost8_withDropOf5_triggers() {
        WeatherAlert a = WeatherAlertRules.evaluate(flat(13, 8, 9, 12), now(), ZONE);
        assertNotNull(a);
        assertEquals("11日起最低 8°C，注意大棚保温", a.shortText);
        assertEquals("11–12 日受冷空气影响，最低 8°C，大棚夜间注意加盖保温被", a.detailText);
    }

    @Test
    public void lowTempAtMost8_withDropOf4_doesNotTrigger() {
        assertNull(WeatherAlertRules.evaluate(flat(12, 8, 9, 12), now(), ZONE));
        assertNull(WeatherAlertRules.evaluate(flat(14, 9, 9, 12), now(), ZONE));   // 9°C > 8，降幅 5 < 8
    }

    @Test
    public void onlyNext3DaysAreChecked() {
        assertNull(WeatherAlertRules.evaluate(flat(20, 19, 18, 17, 5), now(), ZONE));
        assertNotNull(WeatherAlertRules.evaluate(flat(20, 19, 18, 12), now(), ZONE));
    }

    @Test
    public void todayMissing_noAlert() {
        WeatherBundle b = prototype();
        // 缓存是昨天的：今天之前的数据被忽略，以 11 日为「今天」
        assertNull(WeatherAlertRules.evaluate(b, at(20, 9, 0), ZONE));
        WeatherAlert next = WeatherAlertRules.evaluate(b, at(11, 9, 0), ZONE);
        assertNotNull(next);
        assertEquals("12日起最低 8°C，注意大棚保温", next.shortText);
    }

    @Test
    public void spanOfTwoDays_usesLowestTemperature() {
        WeatherBundle b = new WeatherBundle();
        b.daily.add(WeatherFixtures.day(10, 26, 18, "101", "多云", 0, 60, "1-2"));
        b.daily.add(WeatherFixtures.day(11, 16, 9, "101", "多云", 0, 60, "1-2"));
        b.daily.add(WeatherFixtures.day(12, 16, 9, "101", "多云", 0, 60, "1-2"));
        daily(b, 12).tempMin = 10;
        assertEquals("11–12 日受冷空气影响，最低 9°C，大棚夜间注意加盖保温被",
                WeatherAlertRules.evaluate(b, now(), ZONE).detailText);
    }
}
