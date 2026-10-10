package com.heban.app.data.repository;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.local.prefs.CurrentUser;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/** LedgerRepository 的本地实现（Room）。M2 数据层实现前为占位。 */
public final class LocalLedgerRepository implements LedgerRepository {
    private final AppDatabase db;
    private final CurrentUser user;

    public LocalLedgerRepository(AppDatabase db, CurrentUser user) {
        this.db = db;
        this.user = user;
    }

    @Override
    public Single<Long> addEntry(LedgerEntryEntity entry) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable deleteEntry(long entryId) {
        return Completable.complete();
    }
}
