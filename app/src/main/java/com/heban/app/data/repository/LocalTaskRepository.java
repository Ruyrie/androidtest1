package com.heban.app.data.repository;

import android.content.Context;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.TaskItem;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * TaskRepository 的本地实现（Room + WorkManager 提醒）。
 * 查询随当前账号切换（未登录 / 游客为空），写操作要求已登录；所有操作在 io 线程执行。
 */
public final class LocalTaskRepository implements TaskRepository {
    private final Context app;
    private final AppDatabase db;
    private final CurrentUser user;

    public LocalTaskRepository(Context app, AppDatabase db, CurrentUser user) {
        this.app = app;
        this.db = db;
        this.user = user;
    }

    private static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Flowable<List<TaskItem>> observeTasksOn(LocalDate day) {
        return UserScope.observe(user, Collections::<TaskItem>emptyList, uid -> {
            long[] range = DateRanges.day(day, zone());
            return db.farmTaskDao().observeRowsBetween(uid, range[0], range[1]).map(TaskItems::from);
        }).distinctUntilChanged();
    }

    @Override
    public Flowable<List<TaskItem>> observeToday() {
        return Flowable.defer(() -> observeTasksOn(LocalDate.now(zone())));
    }

    @Override
    public Flowable<Map<LocalDate, Set<String>>> observeMonthTypes(YearMonth month) {
        return UserScope.observe(user, Collections::<LocalDate, Set<String>>emptyMap, uid -> {
            ZoneId zone = zone();
            long[] range = DateRanges.month(month, zone);
            return db.farmTaskDao().observeTypesBetween(uid, range[0], range[1])
                    .map(rows -> TaskItems.monthTypes(rows, zone));
        }).distinctUntilChanged();
    }

    @Override
    public Flowable<Integer> observeWeekCount() {
        return UserScope.observe(user, () -> 0, uid -> {
            long[] week = DateRanges.week(LocalDate.now(zone()), zone());
            return db.farmTaskDao().observeCountBetween(uid, week[0], week[1]);
        }).distinctUntilChanged();
    }

    /**
     * id == 0：新增，repeatEveryDays > 0 且 repeatTimes > 1 时生成 repeatTimes 条（同一 seriesId = 创建时间）；
     * id != 0：只修改这一条（完成状态与系列保持不变）。之后为每条安排或取消提醒。返回第一条的 id。
     */
    @Override
    public Single<Long> saveTask(FarmTaskEntity task, int repeatEveryDays, int repeatTimes) {
        return UserScope.require(user).map(uid -> {
            long now = System.currentTimeMillis();
            List<FarmTaskEntity> saved = db.runInTransaction(() -> {
                if (task.id != 0) {
                    FarmTaskEntity old = db.farmTaskDao().getSync(task.id, uid);
                    if (old == null) throw UserScope.notFound("农事不存在");
                    task.userId = uid;
                    task.createdAt = old.createdAt;
                    task.seriesId = old.seriesId;
                    task.done = old.done;
                    task.doneAt = old.doneAt;
                    db.farmTaskDao().updateSync(task);
                    return Collections.singletonList(task);
                }
                List<Long> dueTimes = TaskSchedule.occurrences(task.dueAt, repeatEveryDays, repeatTimes, zone());
                long seriesId = dueTimes.size() > 1 ? now : 0;
                List<FarmTaskEntity> list = new ArrayList<>(dueTimes.size());
                for (Long due : dueTimes) {
                    FarmTaskEntity t = TaskSchedule.copyOf(task);
                    t.userId = uid;
                    t.createdAt = now;
                    t.seriesId = seriesId;
                    t.dueAt = due;
                    t.done = false;
                    t.doneAt = 0;
                    list.add(t);
                }
                List<Long> ids = db.farmTaskDao().insertAllSync(list);
                for (int i = 0; i < list.size(); i++) list.get(i).id = ids.get(i);
                FarmTaskEntity first = list.get(0);
                task.id = first.id;
                task.userId = uid;
                task.createdAt = now;
                task.seriesId = seriesId;
                return list;
            });
            for (FarmTaskEntity t : saved) TaskReminders.sync(app, t, now);
            return saved.get(0).id;
        }).subscribeOn(Schedulers.io());
    }

    /** 完成时取消提醒；取消完成时，提醒时刻还没到则重新安排 */
    @Override
    public Completable setDone(long taskId, boolean done) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() -> {
            long now = System.currentTimeMillis();
            FarmTaskEntity t = db.runInTransaction(() -> {
                FarmTaskEntity x = db.farmTaskDao().getSync(taskId, uid);
                if (x == null) throw UserScope.notFound("农事不存在");
                x.done = done;
                x.doneAt = done ? now : 0;
                db.farmTaskDao().setDoneSync(taskId, uid, x.done, x.doneAt);
                return x;
            });
            TaskReminders.sync(app, t, now);
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable deleteTask(long taskId) {
        return UserScope.require(user).flatMapCompletable(uid -> Completable.fromAction(() -> {
            db.farmTaskDao().deleteSync(taskId, uid);
            TaskReminders.cancel(app, taskId);
        })).subscribeOn(Schedulers.io());
    }
}
