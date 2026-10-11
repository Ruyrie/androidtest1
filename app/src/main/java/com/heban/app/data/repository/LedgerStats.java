package com.heban.app.data.repository;

import com.heban.app.data.local.dao.LedgerBucketSum;
import com.heban.app.data.local.dao.LedgerRow;
import com.heban.app.data.local.dao.LedgerSum;
import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.LedgerDay;
import com.heban.app.data.model.LedgerItem;
import com.heban.app.data.model.LedgerMonthSummary;
import com.heban.app.data.model.MonthBalance;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** D10 收支统计（纯 Java，便于单元测试）：本月合计、结余趋势、支出构成、按天分组。金额单位为分。 */
public final class LedgerStats {
    /** 支出构成中前 3 类之外合并成的类别名（也是记账类别「其他」） */
    public static final String OTHER = "其他";
    /** 支出构成单独列出的类别数 */
    public static final int MIX_TOP = 3;

    private LedgerStats() {
    }

    /** 按收支类型分组的合计 → 本月收入、支出 */
    public static LedgerMonthSummary monthSummary(List<LedgerSum> byType) {
        LedgerMonthSummary s = new LedgerMonthSummary();
        for (LedgerSum row : byType) {
            if (LedgerEntryEntity.TYPE_IN.equals(row.name)) s.incomeCent += row.total;
            else if (LedgerEntryEntity.TYPE_OUT.equals(row.name)) s.expenseCent += row.total;
        }
        return s;
    }

    /**
     * 截至 endMonth 的最近 months 个月的边界（毫秒，本地时区）：返回 months + 1 个值，
     * 第 i 个月为 [bounds[i], bounds[i + 1])，旧 → 新。
     */
    public static long[] monthBounds(YearMonth endMonth, int months, ZoneId zone) {
        long[] bounds = new long[months + 1];
        YearMonth first = endMonth.minusMonths(months - 1L);
        for (int i = 0; i <= months; i++) {
            bounds[i] = DateRanges.startOfDay(first.plusMonths(i).atDay(1), zone);
        }
        return bounds;
    }

    /** 按月份序号分组的收入 / 支出合计 → 每月结余（没有记账的月份为 0），旧 → 新 */
    public static List<MonthBalance> balances(YearMonth endMonth, int months, List<LedgerBucketSum> rows) {
        if (months <= 0) return Collections.emptyList();
        long[] balance = new long[months];
        for (LedgerBucketSum row : rows) {
            if (row.bucket < 0 || row.bucket >= months) continue;
            if (LedgerEntryEntity.TYPE_IN.equals(row.type)) balance[row.bucket] += row.total;
            else if (LedgerEntryEntity.TYPE_OUT.equals(row.type)) balance[row.bucket] -= row.total;
        }
        YearMonth first = endMonth.minusMonths(months - 1L);
        List<MonthBalance> list = new ArrayList<>(months);
        for (int i = 0; i < months; i++) list.add(new MonthBalance(first.plusMonths(i), balance[i]));
        return list;
    }

    /**
     * 支出构成：除「其他」外按金额倒序取前 3 类，其余类别与原有的「其他」合并为「其他」放在最后；
     * 占比按最大余数法取整，合计为 100。没有支出时返回空列表。
     *
     * @param byCategory 按类别分组的支出合计（任意顺序）
     */
    public static List<CategoryShare> expenseMix(List<LedgerSum> byCategory) {
        List<LedgerSum> named = new ArrayList<>();
        long other = 0;
        for (LedgerSum row : byCategory) {
            if (row.total <= 0) continue;
            if (OTHER.equals(row.name) || row.name == null) other += row.total;
            else named.add(row);
        }
        // 稳定排序：金额相同时保持查询顺序
        Collections.sort(named, (a, b) -> Long.compare(b.total, a.total));
        List<String> names = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();
        for (int i = 0; i < named.size(); i++) {
            if (i < MIX_TOP) {
                names.add(named.get(i).name);
                amounts.add(named.get(i).total);
            } else {
                other += named.get(i).total;
            }
        }
        if (other > 0) {
            names.add(OTHER);
            amounts.add(other);
        }
        long[] values = new long[amounts.size()];
        for (int i = 0; i < values.length; i++) values[i] = amounts.get(i);
        int[] percents = percents(values);
        List<CategoryShare> list = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) list.add(new CategoryShare(names.get(i), values[i], percents[i]));
        return list;
    }

    /**
     * 最大余数法：各项占比向下取整后，把剩下的百分点依次分给余数最大的项（余数相同时给靠前的项），
     * 保证合计为 100。合计为 0 时全部为 0。
     */
    public static int[] percents(long[] values) {
        int n = values.length;
        int[] result = new int[n];
        long total = 0;
        for (long v : values) total += v;
        if (total <= 0) return result;
        long[] remainders = new long[n];
        int sum = 0;
        for (int i = 0; i < n; i++) {
            result[i] = (int) (values[i] * 100 / total);
            remainders[i] = values[i] * 100 % total;
            sum += result[i];
        }
        boolean[] used = new boolean[n];
        for (int left = 100 - sum; left > 0; left--) {
            int best = -1;
            for (int i = 0; i < n; i++) {
                if (!used[i] && (best < 0 || remainders[i] > remainders[best])) best = i;
            }
            if (best < 0) break;
            used[best] = true;
            result[best]++;
        }
        return result;
    }

    /**
     * 本月流水按天分组（新 → 旧），每天合计收入、支出；同一天内保持查询顺序（日期倒序）。
     *
     * @param rows 已按日期倒序排列的条目
     */
    public static List<LedgerDay> groupDays(List<LedgerRow> rows, ZoneId zone) {
        Map<LocalDate, LedgerDay> days = new LinkedHashMap<>();
        for (LedgerRow row : rows) {
            LedgerEntryEntity e = row.entry;
            LocalDate date = DateRanges.toDate(e.date, zone);
            LedgerDay day = days.get(date);
            if (day == null) {
                day = new LedgerDay();
                day.date = date;
                days.put(date, day);
            }
            if (LedgerEntryEntity.TYPE_IN.equals(e.type)) day.incomeCent += e.amountCent;
            else day.expenseCent += e.amountCent;
            LedgerItem item = new LedgerItem();
            item.entry = e;
            item.cropName = row.cropName;
            item.plotName = row.plotName;
            day.items.add(item);
        }
        List<LedgerDay> list = new ArrayList<>(days.values());
        Collections.sort(list, (a, b) -> b.date.compareTo(a.date));
        return list;
    }
}
