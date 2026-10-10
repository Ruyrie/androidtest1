package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

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
}
