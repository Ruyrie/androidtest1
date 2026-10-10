package com.heban.app.data.repository;

import androidx.annotation.Nullable;
import androidx.paging.PagingData;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.ProductEntity;
import com.heban.app.data.local.prefs.CurrentUser;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** MarketRepository 的本地实现（Room + Paging 3）。M5 数据层实现前为占位。 */
public final class LocalMarketRepository implements MarketRepository {
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
        return Flowable.just(PagingData.empty());
    }

    @Override
    public Flowable<Integer> count(@Nullable String category) {
        return Flowable.just(0);
    }

    @Override
    public Flowable<List<ProductEntity>> nearby(int limit) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<ProductEntity> observeProduct(long id) {
        return db.productDao().observe(id);
    }

    @Override
    public Completable addView(long id) {
        return Completable.complete();
    }

    @Override
    public Single<Long> publish(ProductEntity product, boolean pin24h) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable delete(long id) {
        return Completable.complete();
    }

    @Override
    public Flowable<List<ProductEntity>> observeMine() {
        return Flowable.just(Collections.emptyList());
    }
}
