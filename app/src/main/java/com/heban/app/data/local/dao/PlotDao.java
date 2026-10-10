package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.PlotEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface PlotDao {
    @Query("SELECT * FROM plot WHERE user_id = :userId ORDER BY created_at")
    Flowable<List<PlotEntity>> observeAll(long userId);

    @Query("SELECT * FROM plot WHERE id = :id")
    Single<PlotEntity> get(long id);

    @Insert
    Single<Long> insert(PlotEntity plot);

    @Update
    Completable update(PlotEntity plot);

    @Query("DELETE FROM plot WHERE id = :id")
    Completable delete(long id);

    // —— 同步方法：只在 io 线程 / 事务中调用 ——

    @Nullable
    @Query("SELECT * FROM plot WHERE id = :id AND user_id = :userId")
    PlotEntity getSync(long id, long userId);

    @Query("SELECT * FROM plot WHERE user_id = :userId ORDER BY created_at")
    List<PlotEntity> listSync(long userId);

    @Insert
    long insertSync(PlotEntity plot);

    @Update
    int updateSync(PlotEntity plot);

    @Query("DELETE FROM plot WHERE id = :id AND user_id = :userId")
    int deleteSync(long id, long userId);

    @Query("UPDATE plot SET indoor_temp = :temp, indoor_humidity = :humidity, reading_at = :at "
            + "WHERE id = :id AND user_id = :userId")
    int updateReadingSync(long id, long userId, double temp, int humidity, long at);
}
