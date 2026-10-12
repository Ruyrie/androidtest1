package com.heban.app.ui.discover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DiscoverFormatTest {

    @Test
    public void balance_signAndThousands() {
        assertEquals("+¥3,460", DiscoverFormat.balance(346_000));
        assertEquals("-¥1,200", DiscoverFormat.balance(-120_000));
        assertEquals("+¥12.5", DiscoverFormat.balance(1_250));
        assertEquals("-¥0.06", DiscoverFormat.balance(-6));
        assertEquals("+¥1,234,567.89", DiscoverFormat.balance(123_456_789));
        assertEquals("¥0", DiscoverFormat.balance(0));
    }

    @Test
    public void money_noSign() {
        assertEquals("¥25", DiscoverFormat.money(2_500));
        assertEquals("¥1,800", DiscoverFormat.money(180_000));
        assertEquals("¥0.6", DiscoverFormat.money(60));
        assertEquals("¥0", DiscoverFormat.money(0));
    }

    @Test
    public void regionShort_takesLastLevel() {
        assertEquals("江宁区", DiscoverFormat.regionShort("江苏省 南京市 江宁区"));
        assertEquals("江宁区", DiscoverFormat.regionShort("  江苏省  南京市  江宁区 "));
        assertEquals("句容市", DiscoverFormat.regionShort("句容市"));
        assertNull(DiscoverFormat.regionShort(null));
        assertNull(DiscoverFormat.regionShort("   "));
    }

    @Test
    public void seasonHeader_regionAndMonth() {
        assertEquals("江宁区 · 10 月", DiscoverFormat.seasonHeader("江苏省 南京市 江宁区", 10));
        assertEquals("10 月", DiscoverFormat.seasonHeader(null, 10));
        assertEquals("3 月", DiscoverFormat.seasonHeader("", 3));
    }

    @Test
    public void distance_dropsTrailingZero() {
        assertEquals("12", DiscoverFormat.distance(12.0));
        assertEquals("6", DiscoverFormat.distance(6));
        assertEquals("0.8", DiscoverFormat.distance(0.8));
        assertEquals("3.5", DiscoverFormat.distance(3.46));
    }

    @Test
    public void cropsDot_joinsTags() {
        assertEquals("黄瓜 · 白菜", DiscoverFormat.cropsDot("黄瓜、白菜"));
        assertEquals("番茄 · 草莓", DiscoverFormat.cropsDot("番茄,草莓"));
        assertEquals("多种蔬菜", DiscoverFormat.cropsDot("多种蔬菜"));
        assertEquals("", DiscoverFormat.cropsDot(null));
        assertEquals("黄瓜", DiscoverFormat.cropsDot("、黄瓜、"));
    }
}
