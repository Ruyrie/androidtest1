package com.heban.app.data.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LocationResolverTest {

    private static List<String> render(List<String[]> qs) {
        List<String> out = new ArrayList<>();
        for (String[] q : qs) out.add(q[0] + "|" + q[1]);
        return out;
    }

    @Test
    public void regionQueries_mostSpecificFirst() {
        List<String> q = render(LocationResolver.regionQueries("江苏省 南京市 江宁区"));
        assertEquals(java.util.Arrays.asList(
                "江宁区|南京", "江宁|南京", "江宁|null",
                "南京市|江苏", "南京|江苏",
                "江苏省|null", "江苏|null"), q);
    }

    @Test
    public void regionQueries_withoutSpaces_isSplitBySuffix() {
        List<String> q = render(LocationResolver.regionQueries("江苏省南京市江宁区"));
        assertEquals("江宁区|南京", q.get(0));
    }

    @Test
    public void regionQueries_shortCountyNameKeepsSuffix() {
        List<String[]> q = LocationResolver.regionQueries("江苏省 徐州市 沛县");
        assertEquals("沛县", q.get(0)[0]);
        assertEquals("徐州", q.get(0)[1]);
        // 「沛」只有一个字，不去掉「县」
        assertEquals("南京", LocationResolver.stripSuffix("南京市"));
        assertEquals("沛县", LocationResolver.stripSuffix("沛县"));
        assertEquals("广西壮族", LocationResolver.stripSuffix("广西壮族自治区"));
    }

    @Test
    public void regionQueries_empty() {
        assertTrue(LocationResolver.regionQueries(null).isEmpty());
        assertTrue(LocationResolver.regionQueries("  ").isEmpty());
    }

    @Test
    public void singlePart_noAdm() {
        List<String[]> q = LocationResolver.regionQueries("江宁");
        assertEquals(1, q.size());
        assertEquals("江宁", q.get(0)[0]);
        assertNull(q.get(0)[1]);
    }

    @Test
    public void lonLat_twoDecimals_usDecimalPoint() {
        assertEquals("118.78,31.95", LocationResolver.lonLat(118.7812, 31.9466));
    }
}
