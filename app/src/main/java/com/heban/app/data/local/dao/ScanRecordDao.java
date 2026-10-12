package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.ScanRecordEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ScanRecordDao {
    @Query("SELECT * FROM scan_record WHERE user_id = :userId ORDER BY created_at DESC")
    Flowable<List<ScanRecordEntity>> observeAll(long userId);

    @Query("SELECT * FROM scan_record WHERE id = :id")
    Single<ScanRecordEntity> get(long id);

    @Insert
    Single<Long> insert(ScanRecordEntity record);

    @Update
    Completable update(ScanRecordEntity record);

    @Query("DELETE FROM scan_record WHERE id = :id")
    Completable delete(long id);

    // —— 同步方法：只在 io 线程 / 事务中调用 ——

    @Nullable
    @Query("SELECT * FROM scan_record WHERE id = :id AND user_id = :userId")
    ScanRecordEntity getSync(long id, long userId);

    @Insert
    long insertSync(ScanRecordEntity record);

    @Update
    int updateSync(ScanRecordEntity record);

    @Query("UPDATE scan_record SET crop_id = :cropId WHERE id = :id AND user_id = :userId")
    int setCropSync(long id, long userId, long cropId);

    @Query("DELETE FROM scan_record WHERE id = :id AND user_id = :userId")
    int deleteSync(long id, long userId);
}
