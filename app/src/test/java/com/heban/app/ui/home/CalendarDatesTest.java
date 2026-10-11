package com.heban.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/** 月历格子（周日开头）与日期换算。 */
public class CalendarDatesTest {

    @Test
    public void october2026_startsOnThursday_fiveRows() {
        YearMonth oct = YearMonth.of(2026, 10);
        assertEquals(4, CalendarDates.leadingBlanks(oct));   // 日 一 二 三 空，周四 1 号
        assertEquals(5, CalendarDates.rows(oct));
        List<LocalDate> cells = CalendarDates.cells(oct);
        assertEquals(35, cells.size());
        for (int i = 0; i < 4; i++) assertNull(cells.get(i));
        assertEquals(LocalDate.of(2026, 10, 1), cells.get(4));
        assertEquals(LocalDate.of(2026, 10, 10), cells.get(13));   // 第 2 行最后一格（周六）
        assertEquals(LocalDate.of(2026, 10, 31), cells.get(34));
    }

    @Test
    public void rows_fourToSix() {
        assertEquals(0, CalendarDates.leadingBlanks(YearMonth.of(2026, 2)));   // 2026-02-01 周日
        assertEquals(4, CalendarDates.rows(YearMonth.of(2026, 2)));            // 28 天正好 4 行
        assertEquals(6, CalendarDates.leadingBlanks(YearMonth.of(2026, 8)));   // 2026-08-01 周六
        assertEquals(6, CalendarDates.rows(YearMonth.of(2026, 8)));            // 6 + 31 = 37 格
        assertEquals(5, CalendarDates.rows(YearMonth.of(2027, 2)));            // 2027-02-01 周一
        assertEquals(29, CalendarDates.cells(YearMonth.of(2024, 2)).size()
                - CalendarDates.leadingBlanks(YearMonth.of(2024, 2)));          // 闰年
    }

    @Test
    public void weekdayIndex_sundayFirst() {
        assertEquals(0, CalendarDates.weekdayIndex(LocalDate.of(2026, 10, 11)));   // 周日
        assertEquals(6, CalendarDates.weekdayIndex(LocalDate.of(2026, 10, 10)));   // 周六
        assertEquals(4, CalendarDates.weekdayIndex(LocalDate.of(2026, 10, 1)));    // 周四
    }

    @Test
    public void pickerSelection_roundTrip() {
        LocalDate d = LocalDate.of(2026, 10, 11);
        assertEquals(1791676800000L, CalendarDates.toPickerSelection(d));
        assertEquals(d, CalendarDates.fromPickerSelection(CalendarDates.toPickerSelection(d)));
    }

    @Test
    public void epochMillis_usesZone() {
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        long ms = CalendarDates.toEpochMillis(LocalDate.of(2026, 10, 11), LocalTime.of(8, 0), shanghai);
        assertEquals(1791676800000L, ms);   // 08:00 北京时间 = 00:00 UTC
        assertEquals(LocalTime.of(8, 0), CalendarDates.timeOf(ms, shanghai));
    }

    @Test
    public void fromEpochDay_nullable() {
        assertNull(CalendarDates.fromEpochDay(null));
        assertEquals(LocalDate.of(2026, 10, 10),
                CalendarDates.fromEpochDay(LocalDate.of(2026, 10, 10).toEpochDay()));
    }
}
