package com.heban.app.data.repository;

import androidx.annotation.Nullable;
import androidx.paging.PagingData;

import com.heban.app.data.local.entity.ProductEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** 农产品集市（D01 / D07–D09，技术准备文档 7.6）。商品列表所有账号共用；发布 / 置顶需要登录。 */
public interface MarketRepository {
    String SORT_DISTANCE = "距离最近";
    String SORT_NEWEST = "最新发布";
    String SORT_PRICE = "价格最低";

    /** D07 分页列表：category 为「全部」或 null 时不过滤；置顶商品始终在最前，其余按 sort 排序 */
    Flowable<PagingData<ProductEntity>> products(@Nullable String category, String sort);

    /** 在售数量（D01「附近 36 件在售」、D07「江宁区附近 · 36 件」），category 规则同上 */
    Flowable<Integer> count(@Nullable String category);

    /** D01「附近好物」：距离最近的 limit 件（不含自己发布的） */
    Flowable<List<ProductEntity>> nearby(int limit);

    Flowable<ProductEntity> observeProduct(long id);

    /** 打开详情时浏览数 + 1 */
    Completable addView(long id);

    /**
     * D09 发布：pin24h 时在同一事务内扣除 100 积分并写入 pinned_until = now + 24h；积分不足抛出
     * BusinessException("POINTS_NOT_ENOUGH", "积分不足")。返回商品 id。
     */
    Single<Long> publish(ProductEntity product, boolean pin24h);

    /** 删除自己发布的商品 */
    Completable delete(long id);

    /** M01「我的发布」 */
    Flowable<List<ProductEntity>> observeMine();
}
