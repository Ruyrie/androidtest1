package com.heban.app.data.repository;

import com.heban.app.data.local.entity.LedgerEntryEntity;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/** 收支记账（M2 只用到写入；统计与列表在 M5 的 D10 / D11 中补充）。按当前账号隔离。 */
public interface LedgerRepository {

    Single<Long> addEntry(LedgerEntryEntity entry);

    Completable deleteEntry(long entryId);
}
