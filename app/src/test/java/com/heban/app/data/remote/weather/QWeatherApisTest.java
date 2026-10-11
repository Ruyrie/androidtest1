package com.heban.app.data.remote.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class QWeatherApisTest {

    @Test
    public void baseUrl_addsHttpsAndSlash() {
        assertEquals("https://abcd.re.qweatherapi.com/", QWeatherApis.baseUrl("abcd.re.qweatherapi.com"));
        assertEquals("https://abcd.re.qweatherapi.com/", QWeatherApis.baseUrl(" https://abcd.re.qweatherapi.com/ "));
        assertEquals("http://127.0.0.1:8080/", QWeatherApis.baseUrl("http://127.0.0.1:8080"));
        assertNull(QWeatherApis.baseUrl(""));
        assertNull(QWeatherApis.baseUrl("   "));
        assertNull(QWeatherApis.baseUrl(null));
    }

    @Test
    public void isConfigured_needsHostAndKey() {
        assertTrue(QWeatherApis.isConfigured("abcd.re.qweatherapi.com", "k"));
        assertFalse(QWeatherApis.isConfigured("abcd.re.qweatherapi.com", ""));
        assertFalse(QWeatherApis.isConfigured("", "k"));
        assertFalse(QWeatherApis.isConfigured(null, null));
    }

    @Test
    public void parseTime_withOffset() {
        long expected = LocalDateTime.of(2021, 2, 16, 15, 0).atZone(ZoneId.of("Asia/Shanghai"))
                .toInstant().toEpochMilli();
        assertEquals(expected, QWeatherMapper.parseTime("2021-02-16T15:00+08:00"));
        assertEquals(expected, QWeatherMapper.parseTime("2021-02-16T07:00:00Z"));
        assertEquals(0, QWeatherMapper.parseTime("bad"));
        assertEquals(0, QWeatherMapper.parseTime(null));
    }

    @Test
    public void parseNumbers_lenient() {
        assertEquals(-3, QWeatherMapper.parseInt("-3", 0));
        assertEquals(7, QWeatherMapper.parseInt("6.6", 0));
        assertEquals(-1, QWeatherMapper.parseInt("", -1));
        assertEquals(0.0, QWeatherMapper.parseDouble(null), 0);
    }
}
