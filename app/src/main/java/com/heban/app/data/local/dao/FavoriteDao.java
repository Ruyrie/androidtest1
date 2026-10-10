package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heban.app.data.local.entity.FavoriteEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface FavoriteDao {
    @Query("SELECT COUNT(*) > 0 FROM favorite WHERE user_id = :userId AND target_type = :type AND target_id = :targetId")
    Flowable<Boolean> observeIsFavorite(long userId, String type, long targetId);

    @Query("SELECT * FROM favorite WHERE user_id = :userId AND target_type = :type ORDER BY created_at DESC")
    Flowable<List<FavoriteEntity>> observeAll(long userId, String type);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insert(FavoriteEntity favorite);

    @Query("DELETE FROM favorite WHERE user_id = :userId AND target_type = :type AND target_id = :targetId")
    Completable delete(long userId, String type, long targetId);
}
