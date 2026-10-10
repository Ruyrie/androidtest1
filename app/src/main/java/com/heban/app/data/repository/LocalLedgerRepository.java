package com.heban.app.data.repository;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.local.prefs.CurrentUser;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** LedgerRepository 的本地实现（Room），按当前账号隔离；未登录时写操作报 NOT_LOGGED_IN。 */
public final class LocalLedgerRepository implements LedgerRepository {
    private final AppDatabase db;
    private final CurrentUser user;

    public LocalLedgerRepository(AppDatabase db, CurrentUser user) {
        this.db = db;
        this.user = user;
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
}
