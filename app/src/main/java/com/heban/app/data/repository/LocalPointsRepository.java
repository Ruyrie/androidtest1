package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.PointsTxnEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.CheckInState;
import com.heban.app.data.model.PointsSummary;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** PointsRepository 的本地实现（points_txn / checkin）。M5 数据层实现前为占位。 */
public final class LocalPointsRepository implements PointsRepository {
    private final AppDatabase db;
    private final CurrentUser user;

    public LocalPointsRepository(AppDatabase db, CurrentUser user) {
        this.db = db;
        this.user = user;
    }

    @Override
    public Flowable<PointsSummary> observeSummary() {
        PointsSummary s = new PointsSummary();
        s.level = 1;
        s.levelTitle = "";
        s.nextLevelGap = PointsRules.nextLevelGap(0);
        return Flowable.just(s);
    }

    @Override
    public Flowable<Integer> observeBalance() {
        return Flowable.just(0);
    }

    @Override
    public Flowable<CheckInState> observeCheckIn() {
        return Flowable.just(new CheckInState());
    }

    @Override
    public Single<Integer> checkIn() {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Single<Integer> award(String type, String title, @Nullable String refId) {
        return Single.just(0);
    }

    @Override
    public Completable grant(String type, String title, int points, int growth, @Nullable String refId) {
        return Completable.complete();
    }

    @Override
    public Completable spend(String type, String title, int points, @Nullable String refId) {
        return Completable.complete();
    }

    @Override
    public void spendInTransaction(long userId, String type, String title, int points, @Nullable String refId) {
    }

    @Override
    public Flowable<List<PointsTxnEntity>> observeTxns(String filter) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<Integer> observeTodayCount(String type) {
        return Flowable.just(0);
    }
}
