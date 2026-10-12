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

    /** 取一次全部条目（名称匹配、作物选项用；百科只有几十条，筛选在内存中完成） */
    @Query("SELECT * FROM pest ORDER BY id")
    Single<List<PestEntity>> getAll();

    @Query("SELECT * FROM pest WHERE id = :id")
    Single<PestEntity> get(long id);

    @Query("SELECT COUNT(*) FROM pest")
    Flowable<Integer> observeCount();

    @Query("SELECT COUNT(*) FROM pest")
    int countSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<PestEntity> pests);
}
