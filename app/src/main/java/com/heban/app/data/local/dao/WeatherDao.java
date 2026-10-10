package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heban.app.data.local.entity.WeatherCacheEntity;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

@Dao
public interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE location_id = :locationId")
    Maybe<WeatherCacheEntity> get(String locationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable upsert(WeatherCacheEntity cache);
}
