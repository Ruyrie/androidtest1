package com.heban.app.data.repository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.local.dao.LedgerBucketSum;
import com.heban.app.data.local.dao.LedgerRow;
import com.heban.app.data.local.dao.LedgerSum;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.LedgerDay;
import com.heban.app.data.model.LedgerMonthSummary;
import com.heban.app.data.model.MonthBalance;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LedgerStatsTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static long at(int y, int m, int d, int h, int min) {
        return LocalDateTime.of(y, m, d, h, min).atZone(ZONE).toInstant().toEpochMilli();
    }

    // —— 本月范围与合计 ——

    @Test
    public void monthBoundsAreLocalMidnights() {
        long[] oct = DateRanges.month(YearMonth.of(2026, 10), ZONE);
        assertEquals(at(2026, 10, 1, 0, 0), oct[0]);
        assertEquals(at(2026, 11, 1, 0, 0), oct[1]);
        // 9 月 30 日 23:59 不属于 10 月；10 月 31 日 23:59 属于 10 月
        assertTrue(at(2026, 9, 30, 23, 59) < oct[0]);
        assertTrue(at(2026, 10, 31, 23, 59) < oct[1]);

        long[] feb = DateRanges.month(YearMonth.of(2028, 2), ZONE);
        assertEquals(at(2028, 3, 1, 0, 0), feb[1]);
    }

    @Test
    public void monthSummaryFromTypeTotals() {
        LedgerMonthSummary s = LedgerStats.monthSummary(Arrays.asList(
                new LedgerSum(LedgerEntryEntity.TYPE_IN, 520000), new LedgerSum(LedgerEntryEntity.TYPE_OUT, 174000)));
        assertEquals(520000, s.incomeCent);
        assertEquals(174000, s.expenseCent);
        assertEquals(346000, s.balanceCent());   // 原型：本月结余 +¥3,460

        LedgerMonthSummary empty = LedgerStats.monthSummary(Collections.emptyList());
        assertEquals(0, empty.balanceCent());
    }

    // —— 结余趋势 ——

    @Test
    public void recentMonthBounds() {
        long[] b = LedgerStats.monthBounds(YearMonth.of(2026, 10), 6, ZONE);
        assertEquals(7, b.length);
        assertEquals(at(2026, 5, 1, 0, 0), b[0]);
        assertEquals(at(2026, 6, 1, 0, 0), b[1]);
        assertEquals(at(2026, 10, 1, 0, 0), b[5]);
        assertEquals(at(2026, 11, 1, 0, 0), b[6]);

        long[] acrossYear = LedgerStats.monthBounds(YearMonth.of(2027, 2), 6, ZONE);
        assertEquals(at(2026, 9, 1, 0, 0), acrossYear[0]);
    }

    @Test
    public void balancesIncludeEmptyMonths() {
        List<LedgerBucketSum> rows = Arrays.asList(
                new LedgerBucketSum(0, LedgerEntryEntity.TYPE_IN, 100000),
                new LedgerBucketSum(0, LedgerEntryEntity.TYPE_OUT, 18000),
                new LedgerBucketSum(3, LedgerEntryEntity.TYPE_OUT, 5000),
                new LedgerBucketSum(5, LedgerEntryEntity.TYPE_IN, 520000),
                new LedgerBucketSum(5, LedgerEntryEntity.TYPE_OUT, 174000));
        List<MonthBalance> list = LedgerStats.balances(YearMonth.of(2026, 10), 6, rows);
        assertEquals(6, list.size());
        assertEquals(YearMonth.of(2026, 5), list.get(0).month);
        assertEquals(82000, list.get(0).balanceCent);
        assertEquals(0, list.get(1).balanceCent);
        assertEquals(0, list.get(2).balanceCent);
        assertEquals(YearMonth.of(2026, 8), list.get(3).month);
        assertEquals(-5000, list.get(3).balanceCent);
        assertEquals(0, list.get(4).balanceCent);
        assertEquals(YearMonth.of(2026, 10), list.get(5).month);
        assertEquals(346000, list.get(5).balanceCent);

        List<MonthBalance> empty = LedgerStats.balances(YearMonth.of(2026, 10), 6, Collections.emptyList());
        assertEquals(6, empty.size());
        for (MonthBalance m : empty) assertEquals(0, m.balanceCent);
        assertEquals(0, LedgerStats.balances(YearMonth.of(2026, 10), 0, rows).size());
    }

    @Test
    public void bucketQueryBindsMonthBoundaries() {
        long[] b = LedgerStats.monthBounds(YearMonth.of(2026, 10), 3, ZONE);
        androidx.sqlite.db.SupportSQLiteQuery q = LocalLedgerRepository.bucketQuery(7, b);
        assertEquals("SELECT CASE WHEN date < ? THEN 0 WHEN date < ? THEN 1 ELSE 2 END AS bucket, type, "
                + "SUM(amount_cent) AS total FROM ledger_entry WHERE user_id = ? AND date >= ? AND date < ? "
                + "GROUP BY bucket, type", q.getSql());
        assertEquals(5, q.getArgCount());

        androidx.sqlite.db.SupportSQLiteQuery one = LocalLedgerRepository.bucketQuery(7,
                LedgerStats.monthBounds(YearMonth.of(2026, 10), 1, ZONE));
        assertTrue(one.getSql().startsWith("SELECT 0 AS bucket"));
        assertEquals(3, one.getArgCount());
    }

    // —— 支出构成 ——

    @Test
    public void mixMatchesPrototype() {
        // 原型：化肥 720 · 41%、农药 410 · 24%、人工 400 · 23%、其他 210 · 12%
        List<CategoryShare> mix = LedgerStats.expenseMix(Arrays.asList(
                new LedgerSum("化肥", 72000), new LedgerSum("农药", 41000),
                new LedgerSum("人工", 40000), new LedgerSum("其他", 21000)));
        assertMix(mix, new String[]{"化肥", "农药", "人工", "其他"},
                new long[]{72000, 41000, 40000, 21000}, new int[]{41, 24, 23, 12});
    }

    @Test
    public void mixMergesRestAndExistingOther() {
        List<CategoryShare> mix = LedgerStats.expenseMix(Arrays.asList(
                new LedgerSum("其他", 30000), new LedgerSum("化肥", 20000), new LedgerSum("地租", 5000),
                new LedgerSum("农药", 15000), new LedgerSum("人工", 10000), new LedgerSum("农机", 20000)));
        // 前 3：化肥 200、农机 200、农药 150；其他 = 300 + 人工 100 + 地租 50 = 450，放在最后
        assertMix(mix, new String[]{"化肥", "农机", "农药", "其他"},
                new long[]{20000, 20000, 15000, 45000}, new int[]{20, 20, 15, 45});
    }

    @Test
    public void mixWithFewCategoriesHasNoOther() {
        List<CategoryShare> mix = LedgerStats.expenseMix(Arrays.asList(
                new LedgerSum("农药", 1000), new LedgerSum("化肥", 2000)));
        assertMix(mix, new String[]{"化肥", "农药"}, new long[]{2000, 1000}, new int[]{67, 33});

        List<CategoryShare> onlyOther = LedgerStats.expenseMix(Collections.singletonList(new LedgerSum("其他", 500)));
        assertMix(onlyOther, new String[]{"其他"}, new long[]{500}, new int[]{100});

        // 四类时第 4 类单独成为「其他」
        List<CategoryShare> four = LedgerStats.expenseMix(Arrays.asList(new LedgerSum("化肥", 400),
                new LedgerSum("农药", 300), new LedgerSum("人工", 200), new LedgerSum("地租", 100)));
        assertMix(four, new String[]{"化肥", "农药", "人工", "其他"},
                new long[]{400, 300, 200, 100}, new int[]{40, 30, 20, 10});
    }

    @Test
    public void mixEmptyWithoutExpense() {
        assertTrue(LedgerStats.expenseMix(Collections.emptyList()).isEmpty());
        assertTrue(LedgerStats.expenseMix(Collections.singletonList(new LedgerSum("化肥", 0))).isEmpty());
    }

    @Test
    public void percentsAlwaysSumToHundred() {
        assertArrayEquals(new int[]{34, 33, 33}, LedgerStats.percents(new long[]{1, 1, 1}));
        assertArrayEquals(new int[]{100}, LedgerStats.percents(new long[]{7}));
        assertArrayEquals(new int[]{0, 0}, LedgerStats.percents(new long[]{0, 0}));
        long[][] samples = {{1, 1, 1, 1, 1, 1, 1}, {999, 1}, {3, 3, 3, 1}, {12345, 6789, 4321, 1111}, {1, 2, 3, 4}};
        for (long[] s : samples) {
            int sum = 0;
            for (int p : LedgerStats.percents(s)) sum += p;
            assertEquals(100, sum);
        }
    }

    private static void assertMix(List<CategoryShare> mix, String[] names, long[] amounts, int[] percents) {
        assertEquals(names.length, mix.size());
        for (int i = 0; i < names.length; i++) {
            assertEquals(names[i], mix.get(i).name);
            assertEquals(amounts[i], mix.get(i).amountCent);
            assertEquals(percents[i], mix.get(i).percent);
        }
    }

    // —— 按天分组 ——

    private static LedgerRow row(long id, String type, String category, long cent, long date, String crop,
                                 String plot, String source) {
        LedgerEntryEntity e = new LedgerEntryEntity();
        e.id = id;
        e.type = type;
        e.category = category;
        e.amountCent = cent;
        e.date = date;
        e.source = source;
        LedgerRow r = new LedgerRow();
        r.entry = e;
        r.cropName = crop;
        r.plotName = plot;
        return r;
    }

    @Test
    public void daysGroupedNewToOld() {
        List<LedgerRow> rows = new ArrayList<>();
        rows.add(row(3, LedgerEntryEntity.TYPE_IN, "农产品销售", 96000, at(2026, 10, 10, 16, 0),
                "番茄", "1号大棚", LedgerEntryEntity.SOURCE_MANUAL));
        rows.add(row(2, LedgerEntryEntity.TYPE_OUT, "化肥", 6000, at(2026, 10, 10, 0, 0),
                "番茄", "1号大棚", LedgerEntryEntity.SOURCE_RECORD));
        rows.add(row(1, LedgerEntryEntity.TYPE_OUT, "人工", 20000, at(2026, 10, 9, 23, 59),
                null, null, LedgerEntryEntity.SOURCE_MANUAL));
        List<LedgerDay> days = LedgerStats.groupDays(rows, ZONE);
        assertEquals(2, days.size());

        LedgerDay today = days.get(0);
        assertEquals(LocalDate.of(2026, 10, 10), today.date);
        assertEquals(96000, today.incomeCent);   // 原型：收 960 · 支 60
        assertEquals(6000, today.expenseCent);
        assertEquals(2, today.items.size());
        assertEquals(3, today.items.get(0).entry.id);
        assertEquals("番茄", today.items.get(0).cropName);
        assertEquals("1号大棚", today.items.get(0).plotName);
        assertEquals(LedgerEntryEntity.SOURCE_RECORD, today.items.get(1).entry.source);

        LedgerDay yesterday = days.get(1);
        assertEquals(LocalDate.of(2026, 10, 9), yesterday.date);
        assertEquals(0, yesterday.incomeCent);
        assertEquals(20000, yesterday.expenseCent);
        assertEquals(null, yesterday.items.get(0).cropName);

        assertTrue(LedgerStats.groupDays(Collections.emptyList(), ZONE).isEmpty());
    }
}
