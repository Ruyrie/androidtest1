package com.heban.app.ui.discover.ledger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.model.CategoryShare;
import com.heban.app.data.model.MonthBalance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D10 两张图的数据与几何（纯 Java，单位 dp，由自定义 View 乘以 density 绘制）：
 * 结余趋势柱状图 {@link LedgerBarChartView}、支出构成横向条形图 {@link LedgerMixChartView}。
 */
public final class LedgerChartModel {
    /** 柱子上方 / 下方数值标签占用的高度：字 16dp + 间距 6dp（原型 .col gap 6px） */
    public static final float LABEL_SPACE_DP = 22f;
    /** 只有正值时最高的柱子高 120dp（原型 v / max * 120），图区 154dp，顶部留白 12dp */
    public static final float TOP_SPARE_DP = 12f;
    /** 非零数值的最小柱高（原型 Math.max(4, …)） */
    public static final float MIN_BAR_DP = 4f;

    private LedgerChartModel() {
    }

    /** 柱状图的一根柱子：月份「5月」、结余（分）、数值标签「+820」 */
    public static final class Bar {
        public final String label;
        public final long cents;
        public final String valueText;

        public Bar(String label, long cents, String valueText) {
            this.label = label;
            this.cents = cents;
            this.valueText = valueText;
        }
    }

    /** 柱子在图区中的位置：baseline 为 0 线；top / bottom 为每根柱子的上下边（0 值时两者都等于 baseline） */
    public static final class BarLayout {
        public final float baseline;
        public final float[] top;
        public final float[] bottom;
        public final boolean hasNegative;

        BarLayout(float baseline, float[] top, float[] bottom, boolean hasNegative) {
            this.baseline = baseline;
            this.top = top;
            this.bottom = bottom;
            this.hasNegative = hasNegative;
        }
    }

    /** 支出构成的一行：类别名、条形长度（相对最大一类 0–1）、「¥720 · 41%」 */
    public static final class MixRow {
        public final String name;
        public final float fraction;
        public final String valueText;

        public MixRow(String name, float fraction, String valueText) {
            this.name = name;
            this.fraction = fraction;
            this.valueText = valueText;
        }
    }

    /** 最近几个月结余（旧 → 新）→ 柱子 */
    @NonNull
    public static List<Bar> bars(@Nullable List<MonthBalance> balances) {
        if (balances == null || balances.isEmpty()) return Collections.emptyList();
        List<Bar> out = new ArrayList<>(balances.size());
        for (MonthBalance b : balances) {
            if (b == null || b.month == null) continue;
            out.add(new Bar(LedgerFormat.monthShort(b.month), b.balanceCent, LedgerFormat.signedAmount(b.balanceCent)));
        }
        return out;
    }

    /**
     * 计算柱子位置。areaHeight 为图区高度（不含下方月份行）。
     * 只有正值（或全为 0）：0 线在图区底部，最高的柱子 = areaHeight − 22 − 12（原型中为 120dp）；
     * 有负值：上下各留 22dp 给数值标签，正负按同一比例分配，0 线以下画负值。
     */
    @NonNull
    public static BarLayout layout(@NonNull long[] values, float areaHeight) {
        long maxPos = 0;
        long maxNeg = 0;
        for (long v : values) {
            if (v > maxPos) maxPos = v;
            if (v < 0 && -v > maxNeg) maxNeg = -v;
        }
        boolean hasNegative = maxNeg > 0;
        float extent = hasNegative ? areaHeight - 2 * LABEL_SPACE_DP : areaHeight - LABEL_SPACE_DP - TOP_SPARE_DP;
        if (extent < 0) extent = 0;
        long range = maxPos + maxNeg;
        float scale = range == 0 ? 0 : extent / range;
        float baseline = hasNegative ? LABEL_SPACE_DP + maxPos * scale : areaHeight;
        float[] top = new float[values.length];
        float[] bottom = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            long v = values[i];
            float h = v == 0 ? 0 : Math.max(MIN_BAR_DP, Math.abs(v) * scale);
            if (v >= 0) {
                top[i] = baseline - h;
                bottom[i] = baseline;
            } else {
                top[i] = baseline;
                bottom[i] = baseline + h;
            }
        }
        return new BarLayout(baseline, top, bottom, hasNegative);
    }

    /** 支出构成 → 横向条形（条长相对最大一类，原型 v / 720） */
    @NonNull
    public static List<MixRow> mixRows(@Nullable List<CategoryShare> shares) {
        if (shares == null || shares.isEmpty()) return Collections.emptyList();
        long max = 0;
        for (CategoryShare s : shares) if (s != null && s.amountCent > max) max = s.amountCent;
        List<MixRow> out = new ArrayList<>(shares.size());
        for (CategoryShare s : shares) {
            if (s == null) continue;
            float fraction = max <= 0 ? 0 : Math.max(0f, Math.min(1f, (float) s.amountCent / max));
            out.add(new MixRow(s.name, fraction, LedgerFormat.mixValue(s.amountCent, s.percent)));
        }
        return out;
    }

    /** 无障碍描述：「近 6 个月结余：5月 820 元，6月 1960 元，…」 */
    @NonNull
    public static String describe(@NonNull List<Bar> bars) {
        List<String> parts = new ArrayList<>(bars.size());
        for (Bar b : bars) {
            String v = (b.cents < 0 ? "−" : "") + new BigDecimal(Math.abs(b.cents))
                    .movePointLeft(2).stripTrailingZeros().toPlainString();
            parts.add(b.label + " " + v + " 元");
        }
        return "近 " + bars.size() + " 个月结余：" + String.join("，", parts);
    }
}
