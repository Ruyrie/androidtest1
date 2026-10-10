package com.heban.app.data.repository;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.LedgerDay;
import com.heban.app.data.model.LedgerMonthSummary;
import com.heban.app.data.model.MonthBalance;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** LedgerRepository 的本地实现（Room），按当前账号隔离；未登录时写操作报 NOT_LOGGED_IN。 */
public final class LocalLedgerRepository implements LedgerRepository {
    private final AppDatabase db;
    private final CurrentUser user;
    /** 手动记一笔按 PointsRules「记一笔账」奖励积分（M5 实现） */
    private final PointsRepository points;

    public LocalLedgerRepository(AppDatabase db, CurrentUser user, PointsRepository points) {
        this.db = db;
        this.user = user;
        this.points = points;
    }

    /** 新增一笔收支；未填日期时取当前时间，source 默认 manual */
    @Override
    public Single<Long> addEntry(LedgerEntryEntity entry) {
        return UserScope.require(user).map(uid -> {
            long now = System.currentTimeMillis();
            entry.id = 0;
            entry.userId = uid;
            entry.createdAt = now;
            if (entry.date == 0) entry.date = now;
            long id = db.ledgerDao().insertSync(entry);
            entry.id = id;
            return id;
        }).subscribeOn(Schedulers.io());
    }

    /** 删除一笔收支；若由农事记录同步生成，解除记录上的关联 */
    @Override
    public Completable deleteEntry(long entryId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() ->
                db.runInTransaction(() -> {
                    db.farmRecordDao().clearLedgerEntrySync(entryId, uid);
                    db.ledgerDao().deleteSync(entryId, uid);
                }))).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<LedgerEntryEntity> getEntry(long entryId) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Flowable<LedgerMonthSummary> observeMonth(YearMonth month) {
        return Flowable.just(new LedgerMonthSummary());
    }

    @Override
    public Flowable<List<MonthBalance>> observeRecentBalances(YearMonth endMonth, int months) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<CategoryShare>> observeExpenseMix(YearMonth month) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<LedgerDay>> observeDays(YearMonth month) {
        return Flowable.just(Collections.emptyList());
    }
}
