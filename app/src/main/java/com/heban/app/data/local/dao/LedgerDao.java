package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.heban.app.data.local.entity.LedgerEntryEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface LedgerDao {
    @Insert
    Single<Long> insert(LedgerEntryEntity entry);

    @Query("DELETE FROM ledger_entry WHERE id = :id")
    Completable delete(long id);

    /** 某账号的全部收支，按日期倒序 */
    @Query("SELECT * FROM ledger_entry WHERE user_id = :userId ORDER BY date DESC, id DESC")
    Flowable<List<LedgerEntryEntity>> observeAll(long userId);

    /** C05 本季投入：每个地块中作物的支出合计（分） */
    @Query("SELECT c.plot_id AS plot_id, SUM(l.amount_cent) AS value FROM ledger_entry l "
            + "INNER JOIN crop c ON c.id = l.crop_id "
            + "WHERE l.user_id = :userId AND l.type = 'OUT' AND c.plot_id IS NOT NULL "
            + "GROUP BY c.plot_id")
    Flowable<List<PlotValue>> observeOutByPlot(long userId);

    /** D10 本月收入 / 支出：[from, to) 内按收支类型（IN / OUT）分组求和 */
    @Query("SELECT type AS name, SUM(amount_cent) AS total FROM ledger_entry "
            + "WHERE user_id = :userId AND date >= :from AND date < :to GROUP BY type")
    Flowable<List<LedgerSum>> observeTypeTotals(long userId, long from, long to);

    /** D10 支出构成：[from, to) 内的支出按类别分组求和，金额倒序 */
    @Query("SELECT category AS name, SUM(amount_cent) AS total FROM ledger_entry "
            + "WHERE user_id = :userId AND type = 'OUT' AND date >= :from AND date < :to "
            + "GROUP BY category ORDER BY total DESC, category")
    Flowable<List<LedgerSum>> observeExpenseByCategory(long userId, long from, long to);

    /**
     * D10 结余趋势：按月份序号（bucket）和收支类型分组求和。月份边界按本地时区换算成毫秒后作为参数传入，
     * SQL 由 LocalLedgerRepository 拼接（只含占位符）。
     */
    @RawQuery(observedEntities = LedgerEntryEntity.class)
    Flowable<List<LedgerBucketSum>> observeBucketTotals(SupportSQLiteQuery query);

    /** D10 流水：[from, to) 内的条目 + 作物名 + 地块名，按日期倒序 */
    @Query("SELECT l.*, c.name AS crop_name, p.name AS plot_name FROM ledger_entry l "
            + "LEFT JOIN crop c ON c.id = l.crop_id "
            + "LEFT JOIN plot p ON p.id = c.plot_id "
            + "WHERE l.user_id = :userId AND l.date >= :from AND l.date < :to "
            + "ORDER BY l.date DESC, l.id DESC")
    Flowable<List<LedgerRow>> observeRowsBetween(long userId, long from, long to);

    // —— 同步方法：只在 io 线程 / 事务中调用 ——

    @Nullable
    @Query("SELECT * FROM ledger_entry WHERE id = :id AND user_id = :userId")
    LedgerEntryEntity getSync(long id, long userId);

    @Update
    int updateSync(LedgerEntryEntity entry);

    @Query("SELECT * FROM ledger_entry WHERE user_id = :userId ORDER BY date DESC, id DESC")
    List<LedgerEntryEntity> listSync(long userId);

    @Insert
    long insertSync(LedgerEntryEntity entry);

    @Query("DELETE FROM ledger_entry WHERE id = :id AND user_id = :userId")
    int deleteSync(long id, long userId);

    /** 删除农事记录时一并删除它同步生成的支出 */
    @Query("DELETE FROM ledger_entry WHERE record_id = :recordId AND user_id = :userId")
    int deleteByRecordSync(long recordId, long userId);

    /** 删除作物时保留收支，只解除与作物的关联 */
    @Query("UPDATE ledger_entry SET crop_id = NULL WHERE crop_id = :cropId AND user_id = :userId")
    int clearCropSync(long cropId, long userId);
}
