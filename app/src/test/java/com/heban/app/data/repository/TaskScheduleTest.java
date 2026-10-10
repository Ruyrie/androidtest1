package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.local.entity.FarmTaskEntity;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

public class TaskScheduleTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static long at(ZoneId zone, int y, int m, int d, int h, int min) {
        return LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli();
    }

    @Test
    public void everySevenDaysThreeTimes() {
        // H04：10月11日 08:00，每 7 天，共 3 次
        List<Long> times = TaskSchedule.occurrences(at(ZONE, 2026, 10, 11, 8, 0), 7, 3, ZONE);
        assertEquals(3, times.size());
        assertEquals(at(ZONE, 2026, 10, 11, 8, 0), (long) times.get(0));
        assertEquals(at(ZONE, 2026, 10, 18, 8, 0), (long) times.get(1));
        assertEquals(at(ZONE, 2026, 10, 25, 8, 0), (long) times.get(2));
    }

    @Test
    public void keepsLocalTimeAcrossDaylightSaving() {
        ZoneId ny = ZoneId.of("America/New_York");
        List<Long> times = TaskSchedule.occurrences(at(ny, 2026, 10, 28, 8, 0), 7, 2, ny);
        assertEquals(at(ny, 2026, 11, 4, 8, 0), (long) times.get(1));
    }

    @Test
    public void noRepeat() {
        assertEquals(1, TaskSchedule.occurrences(1000L, 0, 5, ZONE).size());
        assertEquals(1, TaskSchedule.occurrences(1000L, 7, 1, ZONE).size());
        assertEquals(1, TaskSchedule.occurrences(1000L, 7, 0, ZONE).size());
        assertEquals(TaskSchedule.MAX_REPEAT_TIMES, TaskSchedule.occurrenceCount(1, 100_000));
    }

    @Test
    public void reminderRules() {
        long now = at(ZONE, 2026, 10, 10, 12, 0);
        long due = at(ZONE, 2026, 10, 10, 15, 0);
        assertEquals(at(ZONE, 2026, 10, 10, 14, 30), TaskSchedule.remindAt(due, 30));
        assertTrue(TaskSchedule.shouldRemind(false, due, 30, now));
        assertFalse(TaskSchedule.shouldRemind(true, due, 30, now));
        assertFalse(TaskSchedule.shouldRemind(false, due, -1, now));
        assertFalse(TaskSchedule.shouldRemind(false, due, 30, at(ZONE, 2026, 10, 10, 14, 45)));
        assertTrue(TaskSchedule.shouldRemind(false, due, 0, at(ZONE, 2026, 10, 10, 14, 45)));
    }

    @Test
    public void copyOfResetsId() {
        FarmTaskEntity t = new FarmTaskEntity();
        t.id = 9;
        t.title = "给番茄追施钾肥";
        t.cropId = 2L;
        t.remindMinutes = 30;
        t.note = "嘧霉胺 1000 倍液";
        FarmTaskEntity c = TaskSchedule.copyOf(t);
        assertEquals(0, c.id);
        assertEquals(t.title, c.title);
        assertEquals(t.cropId, c.cropId);
        assertEquals(30, c.remindMinutes);
        assertEquals(t.note, c.note);
    }

    @Test
    public void dateRanges() {
        // 2026-10-10 是周六：本周为 10/05（周一）到 10/12（下周一）
        long[] week = DateRanges.week(LocalDate.of(2026, 10, 10), ZONE);
        assertEquals(at(ZONE, 2026, 10, 5, 0, 0), week[0]);
        assertEquals(at(ZONE, 2026, 10, 12, 0, 0), week[1]);
        long[] monday = DateRanges.week(LocalDate.of(2026, 10, 5), ZONE);
        assertEquals(week[0], monday[0]);
        long[] sunday = DateRanges.week(LocalDate.of(2026, 10, 11), ZONE);
        assertEquals(week[0], sunday[0]);
        long[] day = DateRanges.day(LocalDate.of(2026, 10, 10), ZONE);
        assertEquals(at(ZONE, 2026, 10, 10, 0, 0), day[0]);
        assertEquals(at(ZONE, 2026, 10, 11, 0, 0), day[1]);
        long[] month = DateRanges.month(YearMonth.of(2026, 12), ZONE);
        assertEquals(at(ZONE, 2026, 12, 1, 0, 0), month[0]);
        assertEquals(at(ZONE, 2027, 1, 1, 0, 0), month[1]);
        assertEquals(at(ZONE, 2026, 10, 10, 0, 0), DateRanges.truncateToDay(at(ZONE, 2026, 10, 10, 15, 30), ZONE));
    }
}
