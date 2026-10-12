package com.heban.app.data.repository;

import androidx.annotation.Nullable;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingSource;
import androidx.paging.rxjava3.PagingRx;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.FavoriteEntity;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.remote.BusinessException;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * MarketRepository 的本地实现（Room + Paging 3，技术准备文档 7.6）。
 * 列表所有账号共用；发布、删除、我的发布按当前账号，未登录时写操作报 NOT_LOGGED_IN。
 */
public final class LocalMarketRepository implements MarketRepository {
    public static final int PAGE_SIZE = 20;

    private final AppDatabase db;
    private final CurrentUser user;
    private final PointsRepository points;

    public LocalMarketRepository(AppDatabase db, CurrentUser user, PointsRepository points) {
        this.db = db;
        this.user = user;
        this.points = points;
    }

    @Override
    public Flowable<PagingData<ProductEntity>> products(@Nullable String category, String sort) {
        String cat = MarketRules.category(category);
        String s = MarketRules.sort(sort);
        // 每次失效重新创建 PagingSource 时取当前时间，过期的置顶自动回到普通位置
        Pager<Integer, ProductEntity> pager = new Pager<>(new PagingConfig(PAGE_SIZE),
                () -> pagingSource(cat, s, System.currentTimeMillis()));
        return PagingRx.getFlowable(pager);
    }

    private PagingSource<Integer, ProductEntity> pagingSource(@Nullable String category, String sort, long now) {
        if (SORT_NEWEST.equals(sort)) return db.productDao().pagingSource(category, now);
        if (SORT_PRICE.equals(sort)) return db.productDao().pagingByPrice(category, now);
        return db.productDao().pagingByDistance(category, now);
    }

    @Override
    public Flowable<Integer> count(@Nullable String category) {
        return db.productDao().observeCount(MarketRules.category(category)).subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<List<ProductEntity>> nearby(int limit) {
        if (limit <= 0) return Flowable.just(Collections.emptyList());
        return db.productDao().observeNearby(limit).subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<ProductEntity> observeProduct(long id) {
        return db.productDao().observe(id).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable addView(long id) {
        return db.productDao().addView(id).subscribeOn(Schedulers.io());
    }

    /**
     * 发布：写入当前账号、mine = true、发布时间、距离 0，卖家昵称和所在地取自账号资料（交易地点为空时用资料地区）；
     * pin24h 时在同一事务内扣除 PIN_COST 积分（流水「集市置顶「标题」24 小时」，refId 为商品 id），
     * 积分不足时整个发布回滚并报 POINTS_NOT_ENOUGH。
     */
    @Override
    public Single<Long> publish(ProductEntity product, boolean pin24h) {
        return UserScope.require(user).map(uid -> {
            String title = product.title == null ? "" : product.title.trim();
            if (title.isEmpty()) throw new BusinessException("INVALID_PRODUCT", "请填写标题");
            UserEntity owner = loadUser(uid);
            long now = System.currentTimeMillis();
            product.id = 0;
            product.title = title;
            product.userId = uid;
            product.mine = true;
            product.sellerId = 0;
            if (owner != null) {
                if (owner.nickname != null && !owner.nickname.trim().isEmpty()) product.sellerName = owner.nickname;
                String region = MarketRules.sellerRegion(owner.region);
                if (region != null) product.sellerRegion = region;
                product.sellerYears = owner.years;
                if (product.location == null || product.location.trim().isEmpty()) product.location = owner.region;
            }
            product.distanceKm = 0;
            product.views = 0;
            product.wants = 0;
            product.createdAt = now;
            product.pinnedUntil = pin24h ? now + PointsRules.PIN_DURATION_MS : 0;
            long[] id = new long[1];
            db.runInTransaction(() -> {
                id[0] = db.productDao().insertSync(product);
                if (pin24h) {
                    points.spendInTransaction(uid, PointsRules.TYPE_PIN, "集市置顶「" + title + "」24 小时",
                            PointsRules.PIN_COST, String.valueOf(id[0]));
                }
            });
            product.id = id[0];
            return id[0];
        }).subscribeOn(Schedulers.io());
    }

    @Nullable
    private UserEntity loadUser(long uid) {
        try {
            return db.userDao().get(uid).blockingGet();
        } catch (RuntimeException e) {
            return null;    // 本机没有资料（如远程账号尚未缓存）时用商品中已有的卖家信息
        }
    }

    /** 只能删除自己发布的商品；同时清除各账号对它的收藏 */
    @Override
    public Completable delete(long id) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                db.runInTransaction(() -> {
                    if (db.productDao().deleteMineSync(id, uid) == 0) {
                        throw UserScope.notFound("商品不存在或不是你发布的");
                    }
                    db.favoriteDao().deleteTargetSync(FavoriteEntity.TYPE_PRODUCT, id);
                }))).subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<List<ProductEntity>> observeMine() {
        return UserScope.observe(user, Collections::<ProductEntity>emptyList,
                uid -> db.productDao().observeMine(uid));
    }
}
