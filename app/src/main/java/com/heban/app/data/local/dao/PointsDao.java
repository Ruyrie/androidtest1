package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
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

    /** P01「获得」：since 之后积分为正的流水 */
    @Query("SELECT * FROM points_txn WHERE user_id = :userId AND created_at >= :since AND amount > 0 "
            + "ORDER BY created_at DESC, id DESC")
    Flowable<List<PointsTxnEntity>> observeGainTxnsSince(long userId, long since);

    /** P01「消耗」：since 之后积分为负的流水 */
    @Query("SELECT * FROM points_txn WHERE user_id = :userId AND created_at >= :since AND amount < 0 "
            + "ORDER BY created_at DESC, id DESC")
    Flowable<List<PointsTxnEntity>> observeUseTxnsSince(long userId, long since);

    /** 余额、成长值，以及 [from, to)（本月）内获得与消耗的积分（消耗为正数） */
    @Query("SELECT COALESCE(SUM(amount), 0) AS balance, COALESCE(SUM(growth), 0) AS growth, "
            + "COALESCE(SUM(CASE WHEN amount > 0 AND created_at >= :from AND created_at < :to "
            + "THEN amount ELSE 0 END), 0) AS month_gain, "
            + "COALESCE(SUM(CASE WHEN amount < 0 AND created_at >= :from AND created_at < :to "
            + "THEN -amount ELSE 0 END), 0) AS month_spend "
            + "FROM points_txn WHERE user_id = :userId")
    Flowable<PointsTotals> observeTotals(long userId, long from, long to);

    /** H05 每日任务：[from, to) 内某类流水的条数 */
    @Query("SELECT COUNT(*) FROM points_txn WHERE user_id = :userId AND type = :type "
            + "AND created_at >= :from AND created_at < :to")
    Flowable<Integer> observeCountBetween(long userId, String type, long from, long to);

    @Insert
    long insertTxnSync(PointsTxnEntity txn);

    @Query("SELECT * FROM checkin WHERE user_id = :userId ORDER BY date DESC LIMIT :limit")
    Flowable<List<CheckinEntity>> observeRecentCheckins(long userId, int limit);

    /** H05 签到格子：fromDate（yyyy-MM-dd，含）之后的签到，按日期倒序 */
    @Query("SELECT * FROM checkin WHERE user_id = :userId AND date >= :fromDate ORDER BY date DESC")
    Flowable<List<CheckinEntity>> observeCheckinsSince(long userId, String fromDate);

    @Insert
    void insertCheckinSync(CheckinEntity checkin);

    // —— 同步方法：只在 io 线程 / 事务中调用 ——

    @Query("SELECT COALESCE(SUM(amount), 0) FROM points_txn WHERE user_id = :userId")
    int balanceSync(long userId);

    /** [from, to) 内某类流水的条数（每日次数） */
    @Query("SELECT COUNT(*) FROM points_txn WHERE user_id = :userId AND type = :type "
            + "AND created_at >= :from AND created_at < :to")
    int countBetweenSync(long userId, String type, long from, long to);

    /** [from, to) 内这些类型获得的积分合计（每日任务上限） */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM points_txn WHERE user_id = :userId AND type IN (:types) "
            + "AND amount > 0 AND created_at >= :from AND created_at < :to")
    int sumGainBetweenSync(long userId, List<String> types, long from, long to);

    /** 某天（yyyy-MM-dd）的签到，没有时为 null */
    @Nullable
    @Query("SELECT * FROM checkin WHERE user_id = :userId AND date = :date")
    CheckinEntity getCheckinSync(long userId, String date);
}
