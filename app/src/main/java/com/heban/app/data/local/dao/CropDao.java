package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.CropEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface CropDao {
    @Query("SELECT * FROM crop WHERE user_id = :userId ORDER BY planted_at DESC, id DESC")
    Flowable<List<CropEntity>> observeAll(long userId);

    @Query("SELECT * FROM crop WHERE user_id = :userId ORDER BY planted_at DESC, id DESC")
    Single<List<CropEntity>> listAll(long userId);

    @Query("SELECT * FROM crop WHERE id = :id")
    Single<CropEntity> get(long id);

    @Insert
    Single<Long> insert(CropEntity crop);

    @Update
    Completable update(CropEntity crop);

    @Query("DELETE FROM crop WHERE id = :id")
    Completable delete(long id);
}
