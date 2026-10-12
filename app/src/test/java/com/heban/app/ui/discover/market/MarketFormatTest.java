package com.heban.app.ui.discover.market;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.ProductEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class MarketFormatTest {
    private static final long HOUR = 60L * 60 * 1000;
    private static final long NOW = 1_760_000_000_000L;

    @Test
    public void pinnedHoursLeft_roundsUp() {
        assertEquals(18, MarketFormat.pinnedHoursLeft(NOW + 18 * HOUR, NOW));
        assertEquals(18, MarketFormat.pinnedHoursLeft(NOW + 17 * HOUR + 1, NOW));
        assertEquals(24, MarketFormat.pinnedHoursLeft(NOW + 24 * HOUR, NOW));
        assertEquals(1, MarketFormat.pinnedHoursLeft(NOW + 1000, NOW));
        assertEquals(0, MarketFormat.pinnedHoursLeft(NOW, NOW));
        assertEquals(0, MarketFormat.pinnedHoursLeft(0, NOW));
    }

    @Test
    public void isPinned_onlyBeforeExpiry() {
        ProductEntity p = new ProductEntity();
        p.pinnedUntil = NOW + HOUR;
        assertTrue(MarketFormat.isPinned(p, NOW));
        assertFalse(MarketFormat.isPinned(p, NOW + HOUR));
        p.pinnedUntil = 0;
        assertFalse(MarketFormat.isPinned(p, NOW));
    }

    @Test
    public void price_formatsLikePrototype() {
        assertEquals("8", MarketFormat.price(800));
        assertEquals("25", MarketFormat.price(2500));
        assertEquals("1,800", MarketFormat.price(180000));
        assertEquals("0.6", MarketFormat.price(60));
        assertEquals("8.5", MarketFormat.price(850));
        assertEquals("38", MarketFormat.price(3800));
    }

    @Test
    public void parsePriceCent_validatesInput() {
        assertEquals(2500, MarketFormat.parsePriceCent("25"));
        assertEquals(60, MarketFormat.parsePriceCent("0.6"));
        assertEquals(180000, MarketFormat.parsePriceCent("1,800"));
        assertEquals(-1, MarketFormat.parsePriceCent("0"));
        assertEquals(-1, MarketFormat.parsePriceCent(""));
        assertEquals(-1, MarketFormat.parsePriceCent(null));
        assertEquals(-1, MarketFormat.parsePriceCent("abc"));
        assertEquals(-1, MarketFormat.parsePriceCent("1.234"));
        assertEquals(-1, MarketFormat.parsePriceCent("-3"));
    }

    @Test
    public void numbersAndRegions() {
        assertEquals("12", MarketFormat.number(12.0));
        assertEquals("1.5", MarketFormat.number(1.5));
        assertEquals("1,280", MarketFormat.points(1280));
        assertEquals("江宁区", MarketFormat.district("江苏省 南京市 江宁区"));
        assertNull(MarketFormat.district(null));
        assertEquals("句容白兔镇", MarketFormat.shortRegion("江苏省 句容市 白兔镇"));
        assertEquals("南京江宁区", MarketFormat.shortRegion("江苏省 南京市 江宁区"));
        assertEquals("", MarketFormat.shortRegion(null));
        assertEquals("草", MarketFormat.avatarLetter("草莓姐姐"));
        assertEquals("?", MarketFormat.avatarLetter(" "));
    }

    @Test
    public void jsonList_toleratesBadInput() {
        Gson gson = new Gson();
        assertEquals(Arrays.asList("a.jpg", "b.jpg"), MarketFormat.jsonList(gson, "[\"a.jpg\",\" \",\"b.jpg\"]"));
        assertEquals(Collections.emptyList(), MarketFormat.jsonList(gson, null));
        assertEquals(Collections.emptyList(), MarketFormat.jsonList(gson, "not json"));
    }
}
