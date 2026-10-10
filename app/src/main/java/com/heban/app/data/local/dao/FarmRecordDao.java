package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.FarmRecordEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface FarmRecordDao {
    @Query("SELECT * FROM farm_record WHERE crop_id = :cropId ORDER BY date DESC, id DESC")
    Flowable<List<FarmRecordEntity>> observeByCrop(long cropId);

    @Insert
    Single<Long> insert(FarmRecordEntity record);

    @Query("DELETE FROM farm_record WHERE id = :id")
    Completable delete(long id);

    /** C05：每个地块中作物的最近一次灌溉时间（value = date 毫秒） */
    @Query("SELECT c.plot_id AS plot_id, MAX(r.date) AS value FROM farm_record r "
            + "INNER JOIN crop c ON c.id = r.crop_id "
            + "WHERE r.user_id = :userId AND r.type = 'IRRIGATE' AND c.plot_id IS NOT NULL "
            + "GROUP BY c.plot_id")
    Flowable<List<PlotValue>> observeLastIrrigationByPlot(long userId);

    // —— 同步方法：只在 io 线程 / 事务中调用 ——

    @Nullable
    @Query("SELECT * FROM farm_record WHERE id = :id AND user_id = :userId")
    FarmRecordEntity getSync(long id, long userId);

    @Query("SELECT * FROM farm_record WHERE crop_id = :cropId ORDER BY date DESC, id DESC")
    List<FarmRecordEntity> listByCropSync(long cropId);

    @Insert
    long insertSync(FarmRecordEntity record);

    @Update
    int updateSync(FarmRecordEntity record);

    @Query("DELETE FROM farm_record WHERE id = :id AND user_id = :userId")
    int deleteSync(long id, long userId);

    @Query("DELETE FROM farm_record WHERE crop_id = :cropId AND user_id = :userId")
    int deleteByCropSync(long cropId, long userId);

    /** 记账条目被删除时解除关联 */
    @Query("UPDATE farm_record SET ledger_entry_id = NULL WHERE ledger_entry_id = :entryId AND user_id = :userId")
    int clearLedgerEntrySync(long entryId, long userId);
}
