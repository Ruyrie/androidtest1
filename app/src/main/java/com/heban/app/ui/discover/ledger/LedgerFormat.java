package com.heban.app.ui.discover.ledger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.model.LedgerItem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** D10 / D11 的文字格式（金额、月份、日期、流水副标题），写法与原型一致。纯 Java，便于单元测试。 */
public final class LedgerFormat {
    /** 原型中的分隔符「 · 」 */
    public static final String SEP = " · ";
    /** 支出写「−」（U+2212），收入写「+」（UI 设计文档表 7-1） */
    public static final String MINUS = "−";
    public static final String PLUS = "+";
    public static final String YUAN = "¥";
    /** 来自农事记录同步生成的账目，副标题末尾标注来源 */
    public static final String FROM_RECORD = "来自农事记录";

    private static final String[] WEEKDAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final DateTimeFormatter MONTH_DAY_SLASH = DateTimeFormatter.ofPattern("MM/dd", Locale.CHINA);
    private static final DateTimeFormatter YEAR_MONTH_DAY_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.CHINA);

    private LedgerFormat() {
    }

    /** 分 → 元，千分位、去掉多余的 0：346000 →「3,460」，1250 →「12.5」，1205 →「12.05」 */
    @NonNull
    public static String yuan(long cents) {
        DecimalFormat f = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return f.format(Math.abs(cents) / 100.0);
    }

    /** 「¥5,200」（取绝对值） */
    @NonNull
    public static String money(long cents) {
        return YUAN + yuan(cents);
    }

    /** 结余：「+¥3,460」「−¥280」「¥0」 */
    @NonNull
    public static String signedMoney(long cents) {
        return sign(cents) + YUAN + yuan(cents);
    }

    /** 柱状图数值：「+3,460」「−280」「0」 */
    @NonNull
    public static String signedAmount(long cents) {
        return sign(cents) + yuan(cents);
    }

    /** 流水金额：收入「+960」，支出「−60」 */
    @NonNull
    public static String entryAmount(@Nullable String type, long cents) {
        return (isIncome(type) ? PLUS : MINUS) + yuan(cents);
    }

    public static boolean isIncome(@Nullable String type) {
        return LedgerEntryEntity.TYPE_IN.equals(type);
    }

    /** 「2026年10月」 */
    @NonNull
    public static String monthTitle(@NonNull YearMonth month) {
        return month.getYear() + "年" + month.getMonthValue() + "月";
    }

    /** 「10月」 */
    @NonNull
    public static String monthShort(@NonNull YearMonth month) {
        return month.getMonthValue() + "月";
    }

    /** 按天分组标题：「今天 · 10月10日」「昨天 · 10月9日」「周三 · 10月8日」 */
    @NonNull
    public static String dayTitle(@NonNull LocalDate day, @NonNull LocalDate today) {
        String prefix;
        if (day.equals(today)) prefix = "今天";
        else if (day.equals(today.minusDays(1))) prefix = "昨天";
        else prefix = WEEKDAYS[day.getDayOfWeek().getValue() - 1];
        return prefix + SEP + day.getMonthValue() + "月" + day.getDayOfMonth() + "日";
    }

    /** 按天合计：「收 960 · 支 60」；只有一种时只写一种 */
    @NonNull
    public static String daySummary(long incomeCents, long expenseCents) {
        List<String> parts = new ArrayList<>();
        if (incomeCents != 0 || expenseCents == 0) parts.add("收 " + yuan(incomeCents));
        if (expenseCents != 0) parts.add("支 " + yuan(expenseCents));
        return String.join(SEP, parts);
    }

    /** 流水副标题中的类别简称：「农产品销售」写作「销售」（原型 Ledger），其余不变 */
    @NonNull
    public static String categoryLabel(@Nullable String category) {
        if (category == null || category.isEmpty()) return "其他";
        return "农产品销售".equals(category) ? "销售" : category;
    }

    /** 流水标题：备注（如「樱桃番茄 120 斤」），没有备注时写类别 */
    @NonNull
    public static String itemTitle(@NonNull LedgerItem item) {
        LedgerEntryEntity e = item.entry;
        String note = e == null || e.note == null ? "" : e.note.trim();
        if (!note.isEmpty()) return note;
        return e == null || e.category.isEmpty() ? "其他" : e.category;
    }

    /** 流水副标题：「销售 · 番茄 · 1号大棚」；来自农事记录：「化肥 · 番茄 · 来自农事记录」 */
    @NonNull
    public static String itemSub(@NonNull LedgerItem item) {
        LedgerEntryEntity e = item.entry;
        List<String> parts = new ArrayList<>();
        parts.add(categoryLabel(e == null ? null : e.category));
        if (notBlank(item.cropName)) parts.add(item.cropName.trim());
        if (isFromRecord(e)) {
            parts.add(FROM_RECORD);
        } else if (notBlank(item.plotName)) {
            parts.add(item.plotName.trim());
        }
        return String.join(SEP, parts);
    }

    public static boolean isFromRecord(@Nullable LedgerEntryEntity e) {
        return e != null && LedgerEntryEntity.SOURCE_RECORD.equals(e.source);
    }

    /** 支出构成右侧：「¥720 · 41%」 */
    @NonNull
    public static String mixValue(long cents, int percent) {
        return money(cents) + SEP + percent + "%";
    }

    /** D11 金额上方：「支出 · 化肥」「收入 · 农产品销售」 */
    @NonNull
    public static String entryHeader(@Nullable String type, @Nullable String category) {
        return (isIncome(type) ? "收入" : "支出") + SEP + (category == null ? "" : category);
    }

    /** D11 大号金额：支出「−¥60」，收入「+¥60」；金额字符串按键盘输入原样显示 */
    @NonNull
    public static String amountDisplay(@Nullable String type, @Nullable String amount) {
        String a = amount == null || amount.isEmpty() ? AmountInput.ZERO : amount;
        return (isIncome(type) ? PLUS : MINUS) + YUAN + a;
    }

    /** D11 日期 Chip：「今天 10/10」「昨天 10/09」「10/08」，跨年「2025/10/08」 */
    @NonNull
    public static String dateChip(@NonNull LocalDate date, @NonNull LocalDate today) {
        if (date.equals(today)) return "今天 " + MONTH_DAY_SLASH.format(date);
        if (date.equals(today.minusDays(1))) return "昨天 " + MONTH_DAY_SLASH.format(date);
        if (date.getYear() != today.getYear()) return YEAR_MONTH_DAY_SLASH.format(date);
        return MONTH_DAY_SLASH.format(date);
    }

    /** 作物 Chip：「番茄 · 1号大棚」，没有地块时只写作物名 */
    @NonNull
    public static String cropLabel(@Nullable String cropName, @Nullable String plotName) {
        String name = cropName == null ? "" : cropName.trim();
        return notBlank(plotName) ? name + SEP + plotName.trim() : name;
    }

    private static String sign(long cents) {
        if (cents > 0) return PLUS;
        if (cents < 0) return MINUS;
        return "";
    }

    private static boolean notBlank(@Nullable String s) {
        return s != null && !s.trim().isEmpty();
    }
}
