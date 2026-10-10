package com.heban.app.data.repository;

import android.content.Context;

import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.TaskItem;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** TaskRepository 的本地实现（Room + WorkManager 提醒）。M2 数据层实现前为占位。 */
public final class LocalTaskRepository implements TaskRepository {
    private final Context app;
    private final AppDatabase db;
    private final CurrentUser user;

    public LocalTaskRepository(Context app, AppDatabase db, CurrentUser user) {
        this.app = app;
        this.db = db;
        this.user = user;
    }

    @Override
    public Flowable<List<TaskItem>> observeTasksOn(LocalDate day) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<TaskItem>> observeToday() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<Map<LocalDate, Set<String>>> observeMonthTypes(YearMonth month) {
        return Flowable.just(Collections.emptyMap());
    }

    @Override
    public Flowable<Integer> observeWeekCount() {
        return Flowable.just(0);
    }

    @Override
    public Single<Long> saveTask(FarmTaskEntity task, int repeatEveryDays, int repeatTimes) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable setDone(long taskId, boolean done) {
        return Completable.complete();
    }

    @Override
    public Completable deleteTask(long taskId) {
        return Completable.complete();
    }
}
