package com.heban.app.ui.home;

import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.data.model.TaskItem;
import com.heban.app.data.repository.TaskRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** H03 / H04 测试用的内存 TaskRepository。 */
class CalendarFakeTaskRepository implements TaskRepository {
    final Map<LocalDate, List<TaskItem>> tasksByDay = new HashMap<>();
    final Map<YearMonth, Map<LocalDate, Set<String>>> typesByMonth = new HashMap<>();
    final List<YearMonth> monthRequests = new ArrayList<>();
    final List<LocalDate> dayRequests = new ArrayList<>();
    final List<Long> deleted = new ArrayList<>();
    final Map<Long, Boolean> doneCalls = new HashMap<>();

    FarmTaskEntity savedTask;
    int savedEvery = -1;
    int savedTimes = -1;
    Throwable saveError;

    @Override
    public Flowable<List<TaskItem>> observeTasksOn(LocalDate day) {
        dayRequests.add(day);
        List<TaskItem> list = tasksByDay.get(day);
        return Flowable.just(list == null ? Collections.emptyList() : list);
    }

    @Override
    public Flowable<List<TaskItem>> observeToday() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<Map<LocalDate, Set<String>>> observeMonthTypes(YearMonth month) {
        monthRequests.add(month);
        Map<LocalDate, Set<String>> m = typesByMonth.get(month);
        return Flowable.just(m == null ? Collections.emptyMap() : m);
    }

    @Override
    public Flowable<Integer> observeWeekCount() {
        return Flowable.just(0);
    }

    @Override
    public Single<Long> saveTask(FarmTaskEntity task, int repeatEveryDays, int repeatTimes) {
        if (saveError != null) return Single.error(saveError);
        savedTask = task;
        savedEvery = repeatEveryDays;
        savedTimes = repeatTimes;
        return Single.just(42L);
    }

    @Override
    public Completable setDone(long taskId, boolean done) {
        doneCalls.put(taskId, done);
        return Completable.complete();
    }

    @Override
    public Completable deleteTask(long taskId) {
        deleted.add(taskId);
        return Completable.complete();
    }
}
