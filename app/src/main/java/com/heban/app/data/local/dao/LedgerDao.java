package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

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

    // —— 同步方法：只在 io 线程 / 事务中调用 ——

    @Nullable
    @Query("SELECT * FROM ledger_entry WHERE id = :id AND user_id = :userId")
    LedgerEntryEntity getSync(long id, long userId);

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
