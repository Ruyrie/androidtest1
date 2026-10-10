package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.FarmTaskEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface FarmTaskDao {
    /** [from, to) 时间段内的任务 */
    @Query("SELECT * FROM farm_task WHERE user_id = :userId AND due_at >= :from AND due_at < :to ORDER BY due_at, id")
    Flowable<List<FarmTaskEntity>> observeBetween(long userId, long from, long to);

    @Query("SELECT * FROM farm_task WHERE id = :id")
    Single<FarmTaskEntity> getOnce(long id);

    @Insert
    Single<Long> insert(FarmTaskEntity task);

    @Update
    Completable update(FarmTaskEntity task);

    @Query("DELETE FROM farm_task WHERE id = :id")
    Completable delete(long id);
}
