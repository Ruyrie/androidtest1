package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.heban.app.data.local.entity.CheckinEntity;
import com.heban.app.data.local.entity.PointsTxnEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface PointsDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM points_txn WHERE user_id = :userId")
    Flowable<Integer> observeBalance(long userId);

    @Query("SELECT COALESCE(SUM(growth), 0) FROM points_txn WHERE user_id = :userId")
    Flowable<Integer> observeGrowth(long userId);

    @Query("SELECT * FROM points_txn WHERE user_id = :userId AND created_at >= :since ORDER BY created_at DESC, id DESC")
    Flowable<List<PointsTxnEntity>> observeTxnsSince(long userId, long since);

    @Insert
    long insertTxnSync(PointsTxnEntity txn);

    @Query("SELECT * FROM checkin WHERE user_id = :userId ORDER BY date DESC LIMIT :limit")
    Flowable<List<CheckinEntity>> observeRecentCheckins(long userId, int limit);

    @Insert
    void insertCheckinSync(CheckinEntity checkin);
}
