package com.heban.app.data.repository;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/** 按本地日期换算毫秒区间 [from, to)，查询任务和统计时使用（纯 Java，便于单元测试）。 */
public final class DateRanges {
    private DateRanges() {
    }

    /** 某天 0 点的毫秒值 */
    public static long startOfDay(LocalDate day, ZoneId zone) {
        return day.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /** 毫秒值所在的本地日期 */
    public static LocalDate toDate(long millis, ZoneId zone) {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
    }

    /** 把任意时刻归到当天 0 点 */
    public static long truncateToDay(long millis, ZoneId zone) {
        return startOfDay(toDate(millis, zone), zone);
    }

    /** 某一天：[当天 0 点, 次日 0 点) */
    public static long[] day(LocalDate day, ZoneId zone) {
        return new long[]{startOfDay(day, zone), startOfDay(day.plusDays(1), zone)};
    }

    /** 某天所在的周（周一到周日）：[周一 0 点, 下周一 0 点) */
    public static long[] week(LocalDate day, ZoneId zone) {
        LocalDate monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new long[]{startOfDay(monday, zone), startOfDay(monday.plusDays(7), zone)};
    }

    /** 某月：[1 日 0 点, 下月 1 日 0 点) */
    public static long[] month(YearMonth month, ZoneId zone) {
        return new long[]{startOfDay(month.atDay(1), zone), startOfDay(month.plusMonths(1).atDay(1), zone)};
    }
}
