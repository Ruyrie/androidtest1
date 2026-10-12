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

    // —— 同步版本，供 Repository 在 runInTransaction 中使用 ——

    @Query("SELECT COUNT(*) FROM favorite WHERE user_id = :userId AND target_type = :type AND target_id = :targetId")
    int countSync(long userId, String type, long targetId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertSync(FavoriteEntity favorite);

    @Query("DELETE FROM favorite WHERE user_id = :userId AND target_type = :type AND target_id = :targetId")
    int deleteSync(long userId, String type, long targetId);

    /** 收藏对象被删除时（如删除自己发布的商品），清除所有账号对它的收藏 */
    @Query("DELETE FROM favorite WHERE target_type = :type AND target_id = :targetId")
    int deleteTargetSync(String type, long targetId);
}
