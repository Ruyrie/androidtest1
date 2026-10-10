package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.FarmTaskEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface FarmTaskDao {
    /** [from, to) 时间段内的任务 */
    @Query("SELECT * FROM farm_task WHERE user_id = :userId AND due_at >= :from AND due_at < :to ORDER BY due_at, id")
    Flowable<List<FarmTaskEntity>> observeBetween(long userId, long from, long to);

    @Query("SELECT * FROM farm_task WHERE id = :id")
    Single<FarmTaskEntity> getOnce(long id);

    @Insert
    Single<Long> insert(FarmTaskEntity task);

    @Update
    Completable update(FarmTaskEntity task);

    @Query("DELETE FROM farm_task WHERE id = :id")
    Completable delete(long id);

    /** [from, to) 时间段内的任务，附带作物名称和作物所在地块名称 */
    @Query("SELECT t.id AS id, t.title AS title, t.type AS type, t.crop_id AS crop_id, c.name AS crop_name, "
            + "p.name AS plot_name, t.due_at AS due_at, t.done AS done, t.attention AS attention "
            + "FROM farm_task t LEFT JOIN crop c ON c.id = t.crop_id LEFT JOIN plot p ON p.id = c.plot_id "
            + "WHERE t.user_id = :userId AND t.due_at >= :from AND t.due_at < :to ORDER BY t.due_at, t.id")
    Flowable<List<TaskRow>> observeRowsBetween(long userId, long from, long to);

    /** [from, to) 时间段内的任务数 */
    @Query("SELECT COUNT(*) FROM farm_task WHERE user_id = :userId AND due_at >= :from AND due_at < :to")
    Flowable<Integer> observeCountBetween(long userId, long from, long to);

    /** [from, to) 时间段内每条任务的时间和类型（日历圆点） */
    @Query("SELECT due_at, type FROM farm_task WHERE user_id = :userId AND due_at >= :from AND due_at < :to "
            + "ORDER BY due_at")
    Flowable<List<TaskDayType>> observeTypesBetween(long userId, long from, long to);

    // —— 同步方法：只在 io 线程 / 事务中调用 ——

    @Nullable
    @Query("SELECT * FROM farm_task WHERE id = :id AND user_id = :userId")
    FarmTaskEntity getSync(long id, long userId);

    @Query("SELECT id FROM farm_task WHERE crop_id = :cropId AND user_id = :userId")
    List<Long> idsByCropSync(long cropId, long userId);

    @Insert
    long insertSync(FarmTaskEntity task);

    @Insert
    List<Long> insertAllSync(List<FarmTaskEntity> tasks);

    @Update
    int updateSync(FarmTaskEntity task);

    @Query("UPDATE farm_task SET done = :done, done_at = :doneAt WHERE id = :id AND user_id = :userId")
    int setDoneSync(long id, long userId, boolean done, long doneAt);

    @Query("DELETE FROM farm_task WHERE id = :id AND user_id = :userId")
    int deleteSync(long id, long userId);

    @Query("DELETE FROM farm_task WHERE crop_id = :cropId AND user_id = :userId")
    int deleteByCropSync(long cropId, long userId);
}
