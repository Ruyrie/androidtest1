package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.AddressEntity;
import com.heban.app.data.local.entity.MallItemEntity;
import com.heban.app.data.local.entity.MallOrderEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.LogisticsNode;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** MallRepository 的本地实现（Room，物流按时间推进模拟）。M7 数据层实现前为占位。 */
public final class LocalMallRepository implements MallRepository {
    private final AppDatabase db;
    private final CurrentUser user;
    private final Gson gson;
    private final PointsRepository points;

    public LocalMallRepository(AppDatabase db, CurrentUser user, Gson gson, PointsRepository points) {
        this.db = db;
        this.user = user;
        this.gson = gson;
        this.points = points;
    }

    @Override
    public Flowable<List<MallItemEntity>> observeItems(String type) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<MallItemEntity> observeItem(long itemId) {
        return db.mallDao().observeItem(itemId);
    }

    @Override
    public Flowable<Integer> observeRedeemedQty(long itemId) {
        return Flowable.just(0);
    }

    @Override
    public Single<MallOrderEntity> redeem(long itemId, int qty, @Nullable Long addressId) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Flowable<List<MallOrderEntity>> observeOrders(String tab) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<MallOrderEntity> observeOrder(String orderNo) {
        return db.mallDao().observeOrder(orderNo);
    }

    @Override
    public List<LogisticsNode> logistics(MallOrderEntity order, long now) {
        return Collections.emptyList();
    }

    @Override
    public String statusAt(MallOrderEntity order, long now) {
        return order.status;
    }

    @Override
    public long estimatedAt(MallOrderEntity order) {
        return order.createdAt;
    }

    @Override
    public Completable changeOrderAddress(String orderNo, long addressId) {
        return Completable.complete();
    }

    @Override
    public Flowable<List<AddressEntity>> observeAddresses() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Single<Long> saveAddress(AddressEntity address) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable deleteAddress(long addressId) {
        return Completable.complete();
    }

    @Override
    public Completable setDefaultAddress(long addressId) {
        return Completable.complete();
    }
}
