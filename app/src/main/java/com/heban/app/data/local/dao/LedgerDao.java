package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.heban.app.data.local.entity.LedgerEntryEntity;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface LedgerDao {
    @Insert
    Single<Long> insert(LedgerEntryEntity entry);

    @Query("DELETE FROM ledger_entry WHERE id = :id")
    Completable delete(long id);
}
