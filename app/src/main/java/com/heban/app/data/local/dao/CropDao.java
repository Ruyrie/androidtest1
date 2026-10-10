package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
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

    /** 某账号的一种作物；不存在时发射空列表（便于详情页在作物删除后结束） */
    @Query("SELECT * FROM crop WHERE id = :id AND user_id = :userId")
    Flowable<List<CropEntity>> observeById(long id, long userId);

    /** 某账号的一种作物；不存在时报 EmptyResultSetException */
    @Query("SELECT * FROM crop WHERE id = :id AND user_id = :userId")
    Single<CropEntity> getForUser(long id, long userId);

    // —— 同步方法：只在 io 线程 / 事务中调用 ——

    @Nullable
    @Query("SELECT * FROM crop WHERE id = :id AND user_id = :userId")
    CropEntity getSync(long id, long userId);

    @Insert
    long insertSync(CropEntity crop);

    @Update
    int updateSync(CropEntity crop);

    @Query("DELETE FROM crop WHERE id = :id AND user_id = :userId")
    int deleteSync(long id, long userId);

    /** 删除地块时：地块中的作物 plot_id 置空 */
    @Query("UPDATE crop SET plot_id = NULL WHERE plot_id = :plotId AND user_id = :userId")
    int clearPlotSync(long plotId, long userId);

    @Query("UPDATE crop SET attention_note = :note, attention_source = :source "
            + "WHERE id = :id AND user_id = :userId")
    int setAttentionSync(long id, long userId, @Nullable String note, @Nullable String source);

    @Query("UPDATE crop SET attention_note = NULL, attention_source = NULL, attention_dismissed_at = :at "
            + "WHERE id = :id AND user_id = :userId")
    int dismissAttentionSync(long id, long userId, long at);
}
