package com.heban.app.data.repository;

import com.heban.app.data.local.entity.FarmTaskEntity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** 农事任务的重复日期与提醒时间（H04，纯 Java）。 */
public final class TaskSchedule {
    /** 一次最多生成的重复次数，防止误操作写入过多记录 */
    public static final int MAX_REPEAT_TIMES = 366;

    private TaskSchedule() {
    }

    /** 实际生成的条数：repeatEveryDays > 0 且 repeatTimes > 1 时为 repeatTimes，否则 1 */
    public static int occurrenceCount(int repeatEveryDays, int repeatTimes) {
        if (repeatEveryDays <= 0 || repeatTimes <= 1) return 1;
        return Math.min(repeatTimes, MAX_REPEAT_TIMES);
    }

    /** 每条任务的时间：第一条为 firstDueAt，之后每隔 repeatEveryDays 天（按本地日期加，时刻不变） */
    public static List<Long> occurrences(long firstDueAt, int repeatEveryDays, int repeatTimes, ZoneId zone) {
        int n = occurrenceCount(repeatEveryDays, repeatTimes);
        ZonedDateTime base = Instant.ofEpochMilli(firstDueAt).atZone(zone);
        List<Long> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(base.plusDays((long) i * repeatEveryDays).toInstant().toEpochMilli());
        }
        return out;
    }

    /** 提醒时刻 = 农事时间 − 提前量 */
    public static long remindAt(long dueAt, int remindMinutes) {
        return dueAt - remindMinutes * 60_000L;
    }

    /** 未完成、需要提醒且提醒时刻还没到 */
    public static boolean shouldRemind(boolean done, long dueAt, int remindMinutes, long now) {
        return !done && remindMinutes >= 0 && remindAt(dueAt, remindMinutes) > now;
    }

    public static boolean shouldRemind(FarmTaskEntity task, long now) {
        return shouldRemind(task.done, task.dueAt, task.remindMinutes, now);
    }

    /** 复制一条任务（生成重复任务时使用；id 置 0） */
    public static FarmTaskEntity copyOf(FarmTaskEntity t) {
        FarmTaskEntity c = new FarmTaskEntity();
        c.id = 0;
        c.userId = t.userId;
        c.cropId = t.cropId;
        c.type = t.type;
        c.title = t.title;
        c.dueAt = t.dueAt;
        c.remindMinutes = t.remindMinutes;
        c.seriesId = t.seriesId;
        c.done = t.done;
        c.doneAt = t.doneAt;
        c.attention = t.attention;
        c.note = t.note;
        c.createdAt = t.createdAt;
        return c;
    }
}
