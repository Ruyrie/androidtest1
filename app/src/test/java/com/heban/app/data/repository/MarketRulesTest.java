package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.mock.MockFixture;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MarketRulesTest {
    private static final long NOW = 1_760_000_000_000L;
    private static final long HOUR = 60L * 60 * 1000;

    private static ProductEntity product(long id, double km, long priceCent, long createdAt, long pinnedUntil) {
        ProductEntity p = new ProductEntity();
        p.id = id;
        p.title = "p" + id;
        p.distanceKm = km;
        p.priceCent = priceCent;
        p.createdAt = createdAt;
        p.pinnedUntil = pinnedUntil;
        return p;
    }

    private static List<Long> ids(List<ProductEntity> list) {
        List<Long> out = new ArrayList<>();
        for (ProductEntity p : list) out.add(p.id);
        return out;
    }

    private static List<ProductEntity> sorted(List<ProductEntity> in, String sort) {
        List<ProductEntity> list = new ArrayList<>(in);
        Collections.shuffle(list, new java.util.Random(7));
        Collections.sort(list, MarketRules.comparator(sort, NOW));
        return list;
    }

    private final List<ProductEntity> sample = Arrays.asList(
            product(1, 12, 2500, NOW - 20 * HOUR, 0),
            product(2, 6, 180000, NOW - 9 * HOUR, 0),
            product(3, 3, 3800, NOW - 30 * HOUR, 0),
            product(4, 12, 60, NOW - 52 * HOUR, NOW - HOUR),        // 置顶已过期
            product(5, 0, 800, NOW - 2 * HOUR, NOW + 18 * HOUR),    // 自己发布并置顶
            product(6, 7.2, 1000, NOW - 4 * HOUR, 0),
            product(7, 9.6, 2500, NOW - 46 * HOUR, NOW + HOUR));    // 置顶中

    @Test
    public void pinnedFirstThenDistance() {
        assertEquals(Arrays.asList(5L, 7L, 3L, 2L, 6L, 1L, 4L), ids(sorted(sample, MarketRepository.SORT_DISTANCE)));
    }

    @Test
    public void pinnedFirstThenNewest() {
        assertEquals(Arrays.asList(5L, 7L, 6L, 2L, 1L, 3L, 4L), ids(sorted(sample, MarketRepository.SORT_NEWEST)));
    }

    @Test
    public void pinnedFirstThenPrice() {
        // 价格相同（1 与 7 都是 2500）时新发布的在前；7 置顶所以在最前
        assertEquals(Arrays.asList(5L, 7L, 4L, 6L, 1L, 3L, 2L), ids(sorted(sample, MarketRepository.SORT_PRICE)));
    }

    @Test
    public void tiesFallBackToNewestThenId() {
        List<ProductEntity> list = Arrays.asList(
                product(1, 6, 100, NOW - 70 * HOUR, 0),
                product(2, 6, 100, NOW - 9 * HOUR, 0),
                product(3, 6, 100, NOW - 9 * HOUR, 0));
        assertEquals(Arrays.asList(3L, 2L, 1L), ids(sorted(list, MarketRepository.SORT_DISTANCE)));
    }

    @Test
    public void unknownSortMeansDistance() {
        assertEquals(MarketRepository.SORT_DISTANCE, MarketRules.sort(null));
        assertEquals(MarketRepository.SORT_DISTANCE, MarketRules.sort("随便"));
        assertEquals(MarketRepository.SORT_PRICE, MarketRules.sort("价格最低"));
        assertEquals(MarketRepository.SORT_NEWEST, MarketRules.sort("最新发布"));
    }

    @Test
    public void categoryAllMeansNoFilter() {
        assertNull(MarketRules.category(null));
        assertNull(MarketRules.category("全部"));
        assertNull(MarketRules.category(" "));
        assertEquals("农资", MarketRules.category("农资"));
    }

    @Test
    public void pinnedOnlyWhileNotExpired() {
        assertTrue(MarketRules.isPinned(product(1, 0, 0, 0, NOW + 1), NOW));
        assertFalse(MarketRules.isPinned(product(1, 0, 0, 0, NOW), NOW));
        assertFalse(MarketRules.isPinned(product(1, 0, 0, 0, 0), NOW));
    }

    @Test
    public void sellerRegionUsesDistrict() {
        assertEquals("江宁区", MarketRules.sellerRegion("江苏省 南京市 江宁区"));
        assertEquals("江宁区", MarketRules.sellerRegion("江宁区"));
        assertNull(MarketRules.sellerRegion(" "));
        assertNull(MarketRules.sellerRegion(null));
    }

    @Test
    public void nearbyTwoFromMockData() throws Exception {
        // D01「附近好物」：不含自己发布的，距离最近的 2 件（与 ProductDao.observeNearby 的排序相同）
        List<ProductEntity> all = new ArrayList<>(MockFixture.products(NOW));
        Collections.sort(all, MarketRules.comparator(MarketRepository.SORT_DISTANCE, NOW));
        assertEquals("腐熟羊粪有机肥 40kg 袋装", all.get(0).title);
        assertEquals("小型微耕机 九成新 带旋耕刀", all.get(1).title);
    }
}
