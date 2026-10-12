package com.heban.app.data.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.heban.app.data.local.entity.ProductEntity;

import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProductsJsonTest {
    private static final long NOW = 1_760_000_000_000L;
    private static final long DAY = 24L * 60 * 60 * 1000;
    private final Gson gson = new Gson();

    private static ProductEntity byTitle(List<ProductEntity> all, String title) {
        for (ProductEntity p : all) {
            if (p.title.equals(title)) return p;
        }
        throw new AssertionError("no product " + title);
    }

    @Test
    public void everyProductIsWellFormed() throws Exception {
        List<ProductEntity> all = MockFixture.products(NOW);
        assertTrue(all.size() >= 18 && all.size() <= 24);
        Set<String> cats = new HashSet<>(Arrays.asList(ProductEntity.CAT_PRODUCE, ProductEntity.CAT_SUPPLY,
                ProductEntity.CAT_SEEDLING, ProductEntity.CAT_MACHINE));
        Set<String> usedCats = new HashSet<>();
        Set<Long> ids = new HashSet<>();
        Map<Long, String> sellerNames = new HashMap<>();
        Map<Long, Double> sellerDistance = new HashMap<>();
        for (ProductEntity p : all) {
            String t = p.title;
            assertTrue(t, ids.add(p.id));
            assertTrue(t, p.id > 0);
            assertEquals(t, 0, p.userId);
            assertFalse(t, p.mine);
            assertEquals(t, 0, p.pinnedUntil);
            assertTrue(t, cats.contains(p.category));
            usedCats.add(p.category);
            assertTrue(t, p.sellerId >= 1 && p.sellerId <= 6);
            String prev = sellerNames.put(p.sellerId, p.sellerName);
            assertTrue(t + " seller name", prev == null || prev.equals(p.sellerName));
            Double d = sellerDistance.put(p.sellerId, p.distanceKm);
            assertTrue(t + " seller distance", d == null || d == p.distanceKm);
            assertTrue(t, p.distanceKm > 0);
            assertTrue(t, p.priceCent > 0);
            assertTrue(t, p.minQty >= 1);
            assertNotNull(t, p.unit);
            for (String field : new String[]{p.sellerName, p.sellerRegion, p.subCategory, p.tradeWays, p.location,
                    p.tag, p.description, p.spec, p.stockNote}) {
                assertNotNull(t, field);
                assertFalse(t, field.trim().isEmpty());
            }
            assertTrue(t, p.location.startsWith("江苏省 "));
            String[] images = gson.fromJson(p.images, String[].class);
            assertTrue(t, images.length >= 1);
            for (String img : images) assertTrue(t + " " + img, img.matches("images/products/[a-z0-9_]+\\.jpg"));
            String[] tags = gson.fromJson(p.tags, String[].class);
            assertTrue(t, tags.length >= 1);
            for (String way : p.tradeWays.split("、")) {
                assertTrue(t + " " + way, Arrays.asList("自提", "同城配送", "快递", "面议").contains(way));
            }
            assertTrue(t + " created within two weeks", p.createdAt <= NOW && p.createdAt >= NOW - 14 * DAY);
            assertFalse("user data must not be imported", t.contains("自家大棚樱桃番茄"));
        }
        assertEquals(cats, usedCats);
        assertEquals("草莓姐姐", sellerNames.get(1L));
        assertEquals("王农艺师", sellerNames.get(2L));
        assertEquals("果园阿强", sellerNames.get(3L));
        assertEquals("菜园小周", sellerNames.get(4L));
        assertEquals("江宁老陈", sellerNames.get(5L));
        assertEquals("小林的阳台菜园", sellerNames.get(6L));
    }

    @Test
    public void strawberryMatchesPrototypeDetail() throws Exception {
        ProductEntity p = byTitle(MockFixture.products(NOW), "句容红颜草莓 当天现摘");
        assertEquals(2500, p.priceCent);
        assertEquals("斤", p.unit);
        assertEquals(10, p.minQty, 0);
        assertEquals("草莓姐姐", p.sellerName);
        assertEquals("句容白兔镇", p.sellerRegion);
        assertEquals(8, p.sellerYears);
        assertEquals(98, p.sellerRating);
        assertTrue(p.sellerVerified);
        assertEquals(12, p.distanceKm, 0);
        assertEquals("产地直供", p.tag);
        assertEquals("农产品", p.category);
        assertEquals("水果", p.subCategory);
        assertEquals(Arrays.asList("产地直供", "可自提", "同城配送", "附种植记录"),
                Arrays.asList(gson.fromJson(p.tags, String[].class)));
        assertEquals("大棚种植，膜下滴灌，坐果期只用生物农药。每天早上 6 点采摘，2 斤一盒泡沫箱打包。"
                + "支持来园自提，江宁、句容可当天配送。", p.description);
        assertEquals("江苏省 句容市 白兔镇", p.location);
        assertEquals("2 斤 / 盒，单果 20–30 g", p.spec);
        assertEquals("每日现摘，库存约 300 斤", p.stockNote);
        assertEquals(418, p.views);
        assertEquals(23, p.wants);
        assertEquals(4, gson.fromJson(p.images, String[].class).length);
        assertEquals("images/products/strawberry_fresh.jpg", gson.fromJson(p.images, String[].class)[0]);
    }

    @Test
    public void otherPrototypeProducts() throws Exception {
        List<ProductEntity> all = MockFixture.products(NOW);
        ProductEntity tiller = byTitle(all, "小型微耕机 九成新 带旋耕刀");
        assertEquals(180000, tiller.priceCent);
        assertEquals("果园阿强", tiller.sellerName);
        assertEquals(6, tiller.distanceKm, 0);
        assertEquals("二手", tiller.tag);
        assertEquals(ProductEntity.CAT_MACHINE, tiller.category);

        ProductEntity manure = byTitle(all, "腐熟羊粪有机肥 40kg 袋装");
        assertEquals(3800, manure.priceCent);
        assertEquals("袋", manure.unit);
        assertEquals("菜园小周", manure.sellerName);
        assertEquals(3, manure.distanceKm, 0);
        assertEquals("可配送", manure.tag);
        assertEquals(ProductEntity.CAT_SUPPLY, manure.category);

        ProductEntity seedling = byTitle(all, "红颜草莓脱毒苗 现货");
        assertEquals(60, seedling.priceCent);
        assertEquals("株", seedling.unit);
        assertEquals("草莓姐姐", seedling.sellerName);
        assertEquals(12, seedling.distanceKm, 0);
        assertEquals("种苗", seedling.tag);
        assertEquals(ProductEntity.CAT_SEEDLING, seedling.category);

        ProductEntity tomato = byTitle(all, "大棚樱桃番茄 千禧 现摘");
        assertEquals("images/products/cherry_tomato_box.jpg", gson.fromJson(tomato.images, String[].class)[0]);
    }

    @Test
    public void createdHoursAgoIsRelativeToImportTime() {
        String json = "[{\"id\":7,\"title\":\"t\",\"price_cent\":100,\"images\":[\"a.jpg\"],"
                + "\"seller_verified\":true,\"mine\":true,\"user_id\":9,\"created_hours_ago\":1.5}]";
        List<ProductEntity> list = MockJson.parseProducts(gson, new StringReader(json), NOW);
        ProductEntity p = list.get(0);
        assertEquals(7, p.id);
        assertEquals(NOW - 90 * 60 * 1000L, p.createdAt);
        assertEquals("[\"a.jpg\"]", p.images);
        assertTrue(p.sellerVerified);
        assertFalse("mock products never belong to an account", p.mine);
        assertEquals(0, p.userId);
        assertNull(p.tags);
    }
}
