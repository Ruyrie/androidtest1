package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heban.app.data.local.entity.PestEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface PestDao {
    @Query("SELECT * FROM pest ORDER BY id")
    Flowable<List<PestEntity>> observeAll();

    @Query("SELECT * FROM pest WHERE id = :id")
    Single<PestEntity> get(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<PestEntity> pests);
}
