package com.heban.app.data.repository;

import com.heban.app.data.local.entity.FarmTaskEntity;
import com.heban.app.data.model.TaskItem;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** 农事任务与提醒（H01 / H03 / H04）。按当前账号隔离，规则同 CropRepository。 */
public interface TaskRepository {

    /** 某一天的任务，按时间排序 */
    Flowable<List<TaskItem>> observeTasksOn(LocalDate day);

    /** 今天的任务（H01「今日农事」） */
    Flowable<List<TaskItem>> observeToday();

    /** 某月每天有哪些农事类型（FarmType.code），用于日历圆点 */
    Flowable<Map<LocalDate, Set<String>>> observeMonthTypes(YearMonth month);

    /** 本周（周一到周日）任务数，C01 统计用 */
    Flowable<Integer> observeWeekCount();

    /**
     * 保存任务：repeatEveryDays > 0 且 repeatTimes > 1 时生成 repeatTimes 条（同一 seriesId）。
     * remindMinutes >= 0 时为每条安排提醒（FarmTaskReminderWorker）。返回第一条的 id。
     */
    Single<Long> saveTask(FarmTaskEntity task, int repeatEveryDays, int repeatTimes);

    /** 勾选完成 / 取消完成；完成时取消提醒 */
    Completable setDone(long taskId, boolean done);

    Completable deleteTask(long taskId);
}
