package com.heban.app.data.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.WeatherCacheEntity;
import com.heban.app.data.model.WeatherBundle;

import org.junit.Test;

public class WeatherCacheMapperTest {
    private final Gson gson = new Gson();

    @Test
    public void roundTrip() {
        WeatherBundle b = WeatherFixtures.prototype();
        WeatherCacheEntity e = WeatherCacheMapper.toEntity(b, gson);
        assertEquals("101190104", e.locationId);
        assertEquals("南京 江宁", e.locationName);
        assertEquals(b.fetchedAt, e.fetchedAt);

        WeatherBundle back = WeatherCacheMapper.fromEntity(e, gson);
        assertEquals(b.now.temp, back.now.temp);
        assertEquals("东南风", back.now.windDir);
        assertEquals(24, back.hourly.size());
        assertEquals(b.hourly.get(9).precip, back.hourly.get(9).precip, 1e-9);
        assertEquals(7, back.daily.size());
        assertEquals(7, back.daily.get(3).tempMin);
        assertEquals(b.daily.get(0).date, back.daily.get(0).date);
    }

    @Test
    public void corruptJson_returnsNull() {
        WeatherCacheEntity e = new WeatherCacheEntity();
        e.locationId = "1";
        e.dailyJson = "{not json";
        assertNull(WeatherCacheMapper.fromEntity(e, gson));
        assertNull(WeatherCacheMapper.fromEntity(null, gson));
    }
}
