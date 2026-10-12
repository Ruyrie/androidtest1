package com.heban.app.ui.discover.ledger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.MonthBalance;

import org.junit.Test;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** D10 两张图的数据映射与柱子几何（含负结余）。 */
public class LedgerChartModelTest {
    private static final float EPS = 0.01f;
    /** 图区高度：176dp − 月份行 22dp */
    private static final float AREA = 154f;

    private static List<MonthBalance> balances(YearMonth end, long... yuan) {
        List<MonthBalance> out = new ArrayList<>();
        for (int i = 0; i < yuan.length; i++) {
            out.add(new MonthBalance(end.minusMonths(yuan.length - 1 - i), yuan[i] * 100));
        }
        return out;
    }

    @Test
    public void bars_prototypeValues() {
        List<LedgerChartModel.Bar> bars = LedgerChartModel.bars(
                balances(YearMonth.of(2026, 10), 820, 1960, 3120, 280, 1450, 3460));
        assertEquals(6, bars.size());
        assertEquals("5月", bars.get(0).label);
        assertEquals("10月", bars.get(5).label);
        assertEquals("+820", bars.get(0).valueText);
        assertEquals("+3,460", bars.get(5).valueText);
        assertEquals(346000, bars.get(5).cents);
    }

    @Test
    public void bars_negativeAndZeroText() {
        List<LedgerChartModel.Bar> bars = LedgerChartModel.bars(balances(YearMonth.of(2026, 1), -280, 0));
        assertEquals("12月", bars.get(0).label);
        assertEquals("1月", bars.get(1).label);
        assertEquals("−280", bars.get(0).valueText);
        assertEquals("0", bars.get(1).valueText);
    }

    @Test
    public void bars_emptyOrNull() {
        assertTrue(LedgerChartModel.bars(null).isEmpty());
        assertTrue(LedgerChartModel.bars(Collections.emptyList()).isEmpty());
    }

    @Test
    public void layout_positiveOnly_matchesPrototypeHeights() {
        long[] v = {82000, 196000, 312000, 28000, 145000, 346000};
        LedgerChartModel.BarLayout l = LedgerChartModel.layout(v, AREA);
        assertFalse(l.hasNegative);
        assertEquals(AREA, l.baseline, EPS);
        // 最高的柱子 120dp（原型 v / 3460 * 120）
        assertEquals(120f, l.bottom[5] - l.top[5], EPS);
        assertEquals(820f / 3460f * 120f, l.bottom[0] - l.top[0], EPS);
        assertEquals(280f / 3460f * 120f, l.bottom[3] - l.top[3], EPS);
        for (int i = 0; i < v.length; i++) assertEquals(AREA, l.bottom[i], EPS);
    }

    @Test
    public void layout_smallValue_hasMinimumHeight() {
        LedgerChartModel.BarLayout l = LedgerChartModel.layout(new long[]{100, 1000000}, AREA);
        assertEquals(LedgerChartModel.MIN_BAR_DP, l.bottom[0] - l.top[0], EPS);
    }

    @Test
    public void layout_zero_drawsNothing() {
        LedgerChartModel.BarLayout l = LedgerChartModel.layout(new long[]{0, 0, 0}, AREA);
        assertFalse(l.hasNegative);
        for (int i = 0; i < 3; i++) assertEquals(l.top[i], l.bottom[i], EPS);
    }

    @Test
    public void layout_negative_drawnBelowBaseline() {
        // 5月 820，8月 −280，10月 3460
        long[] v = {82000, 196000, 312000, -28000, 145000, 346000};
        LedgerChartModel.BarLayout l = LedgerChartModel.layout(v, AREA);
        assertTrue(l.hasNegative);
        float extent = AREA - 2 * LedgerChartModel.LABEL_SPACE_DP;
        float scale = extent / (3460f + 280f);
        assertEquals(LedgerChartModel.LABEL_SPACE_DP + 3460f * scale, l.baseline, EPS);
        // 正值在 0 线上方
        assertEquals(l.baseline, l.bottom[5], EPS);
        assertEquals(l.baseline - 3460f * scale, l.top[5], EPS);
        assertEquals(LedgerChartModel.LABEL_SPACE_DP, l.top[5], EPS);
        // 负值从 0 线向下
        assertEquals(l.baseline, l.top[3], EPS);
        assertEquals(l.baseline + 280f * scale, l.bottom[3], EPS);
        // 下方留出数值标签的位置
        assertEquals(AREA - LedgerChartModel.LABEL_SPACE_DP, l.bottom[3], EPS);
    }

    @Test
    public void layout_allNegative() {
        LedgerChartModel.BarLayout l = LedgerChartModel.layout(new long[]{-100000, -50000}, AREA);
        assertTrue(l.hasNegative);
        assertEquals(LedgerChartModel.LABEL_SPACE_DP, l.baseline, EPS);
        assertEquals(AREA - LedgerChartModel.LABEL_SPACE_DP, l.bottom[0], EPS);
        assertTrue(l.bottom[1] > l.top[1]);
        assertEquals(l.baseline, l.top[1], EPS);
    }

    @Test
    public void mixRows_prototypeValues() {
        List<LedgerChartModel.MixRow> rows = LedgerChartModel.mixRows(Arrays.asList(
                new CategoryShare("化肥", 72000, 41), new CategoryShare("农药", 41000, 24),
                new CategoryShare("人工", 40000, 23), new CategoryShare("其他", 21000, 12)));
        assertEquals(4, rows.size());
        assertEquals("化肥", rows.get(0).name);
        assertEquals(1f, rows.get(0).fraction, EPS);
        assertEquals(410f / 720f, rows.get(1).fraction, EPS);
        assertEquals("¥720 · 41%", rows.get(0).valueText);
        assertEquals("¥210 · 12%", rows.get(3).valueText);
    }

    @Test
    public void mixRows_empty() {
        assertTrue(LedgerChartModel.mixRows(null).isEmpty());
        assertTrue(LedgerChartModel.mixRows(Collections.emptyList()).isEmpty());
    }

    @Test
    public void describe_matchesPrototypeAriaLabel() {
        List<LedgerChartModel.Bar> bars = LedgerChartModel.bars(
                balances(YearMonth.of(2026, 10), 820, 1960, 3120, 280, 1450, 3460));
        assertEquals("近 6 个月结余：5月 820 元，6月 1960 元，7月 3120 元，8月 280 元，9月 1450 元，10月 3460 元",
                LedgerChartModel.describe(bars));
    }
}
