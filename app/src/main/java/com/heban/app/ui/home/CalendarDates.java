package com.heban.app.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** H03 / H04 的日期计算：月历格子（周日开头，7 列）、星期序号、日期与毫秒互转。 */
public final class CalendarDates {
    public static final int COLUMNS = 7;

    private CalendarDates() {
    }

    /** 月初 1 号之前的空格数（周日开头：周日 0、周一 1 … 周六 6） */
    public static int leadingBlanks(@NonNull YearMonth month) {
        return month.atDay(1).getDayOfWeek().getValue() % COLUMNS;
    }

    /** 月历需要的行数（4–6 行） */
    public static int rows(@NonNull YearMonth month) {
        int cells = leadingBlanks(month) + month.lengthOfMonth();
        return (cells + COLUMNS - 1) / COLUMNS;
    }

    /** 月历格子：先是 leadingBlanks 个 null，然后是 1 号到月末（不补尾部空格） */
    @NonNull
    public static List<LocalDate> cells(@NonNull YearMonth month) {
        int blanks = leadingBlanks(month);
        List<LocalDate> out = new ArrayList<>(blanks + month.lengthOfMonth());
        for (int i = 0; i < blanks; i++) out.add(null);
        for (int d = 1; d <= month.lengthOfMonth(); d++) out.add(month.atDay(d));
        return out;
    }

    /** 星期序号，周日 0 … 周六 6（与 R.array.calendar_weekdays 对应） */
    public static int weekdayIndex(@NonNull LocalDate day) {
        return day.getDayOfWeek().getValue() % COLUMNS;
    }

    /** 本地日期 + 时间 → 毫秒时间戳 */
    public static long toEpochMillis(@NonNull LocalDate day, @NonNull LocalTime time, @NonNull ZoneId zone) {
        return LocalDateTime.of(day, time).atZone(zone).toInstant().toEpochMilli();
    }

    /** 毫秒时间戳 → 本地时间 */
    @NonNull
    public static LocalTime timeOf(long epochMillis, @NonNull ZoneId zone) {
        return Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime();
    }

    /** LocalDate → MaterialDatePicker 使用的 UTC 零点毫秒 */
    public static long toPickerSelection(@NonNull LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /** MaterialDatePicker 选中的 UTC 毫秒 → LocalDate */
    @NonNull
    public static LocalDate fromPickerSelection(long utcMillis) {
        return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    /** 从页面参数中取日期（缺省或非法时返回 null） */
    @Nullable
    public static LocalDate fromEpochDay(@Nullable Long epochDay) {
        if (epochDay == null) return null;
        try {
            return LocalDate.ofEpochDay(epochDay);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
