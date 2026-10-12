package com.heban.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.ProductEntity;

import java.util.Comparator;

/**
 * 集市的分类、排序和置顶规则（纯 Java）。{@link #comparator} 与 ProductDao 的分页查询顺序一致：
 * 置顶（pinned_until > now）在前，其余按排序方式，再按发布时间新→旧、id 大→小。
 */
public final class MarketRules {
    public static final String CATEGORY_ALL = "全部";

    private MarketRules() {
    }

    /** 「全部」、null 或空 → null（不过滤），其余原样 */
    @Nullable
    public static String category(@Nullable String category) {
        if (category == null) return null;
        String c = category.trim();
        return c.isEmpty() || CATEGORY_ALL.equals(c) ? null : c;
    }

    /** 未知或空的排序方式按「距离最近」（D07 默认） */
    @NonNull
    public static String sort(@Nullable String sort) {
        if (MarketRepository.SORT_NEWEST.equals(sort)) return MarketRepository.SORT_NEWEST;
        if (MarketRepository.SORT_PRICE.equals(sort)) return MarketRepository.SORT_PRICE;
        return MarketRepository.SORT_DISTANCE;
    }

    public static boolean isPinned(@NonNull ProductEntity p, long now) {
        return p.pinnedUntil > now;
    }

    public static Comparator<ProductEntity> comparator(@Nullable String sort, long now) {
        String s = sort(sort);
        Comparator<ProductEntity> pinned = (a, b) -> Boolean.compare(isPinned(b, now), isPinned(a, now));
        Comparator<ProductEntity> primary;
        if (MarketRepository.SORT_PRICE.equals(s)) {
            primary = (a, b) -> Long.compare(a.priceCent, b.priceCent);
        } else if (MarketRepository.SORT_NEWEST.equals(s)) {
            primary = (a, b) -> 0;
        } else {
            primary = (a, b) -> Double.compare(a.distanceKm, b.distanceKm);
        }
        Comparator<ProductEntity> newest = (a, b) -> Long.compare(b.createdAt, a.createdAt);
        Comparator<ProductEntity> byId = (a, b) -> Long.compare(b.id, a.id);
        return pinned.thenComparing(primary).thenComparing(newest).thenComparing(byId);
    }

    /** 自己发布时的卖家所在地：资料地区「江苏省 南京市 江宁区」取最后一级「江宁区」 */
    @Nullable
    public static String sellerRegion(@Nullable String userRegion) {
        if (userRegion == null) return null;
        String r = userRegion.trim();
        if (r.isEmpty()) return null;
        String[] parts = r.split("\\s+");
        return parts[parts.length - 1];
    }
}
