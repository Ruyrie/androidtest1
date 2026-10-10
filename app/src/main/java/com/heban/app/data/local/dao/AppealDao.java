package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.heban.app.data.local.entity.AppealEntity;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface AppealDao {
    @Insert
    Single<Long> insert(AppealEntity appeal);

    @Query("SELECT * FROM appeal WHERE id = :id")
    Flowable<AppealEntity> observe(long id);
}
