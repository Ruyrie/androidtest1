package com.heban.app.data.local.dao;

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
}
