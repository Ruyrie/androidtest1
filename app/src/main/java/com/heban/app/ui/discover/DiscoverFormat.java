package com.heban.app.ui.discover;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** D01 发现页的文字格式（金额、地区、距离、作物标签），纯 Java，便于单元测试。 */
public final class DiscoverFormat {
    private DiscoverFormat() {
    }

    /** 分 → 「¥1,800」「¥0.6」「¥25」 */
    @NonNull
    public static String money(long cent) {
        return "¥" + amount(Math.abs(cent));
    }

    /** 本月结余：正数带「+」（「+¥3,460」），负数带「-」（「-¥1,200.5」），0 为「¥0」 */
    @NonNull
    public static String balance(long cent) {
        if (cent == 0) return "¥0";
        return (cent > 0 ? "+" : "-") + money(cent);
    }

    private static String amount(long cent) {
        return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US)).format(cent / 100.0);
    }

    /** 「江苏省 南京市 江宁区」→「江宁区」（资料只到区县，取最后一级）；空白返回 null */
    @Nullable
    public static String regionShort(@Nullable String region) {
        if (region == null) return null;
        String[] parts = region.trim().split("\\s+");
        String last = parts[parts.length - 1].trim();
        return last.isEmpty() ? null : last;
    }

    /** 「本季高发」右侧：「江宁区 · 10 月」；游客或没有地区时只显示「10 月」 */
    @NonNull
    public static String seasonHeader(@Nullable String region, int month) {
        String r = regionShort(region);
        return r == null ? month + " 月" : r + " · " + month + " 月";
    }

    /** 距离（公里）：整数不带小数（12），否则一位小数（0.8） */
    @NonNull
    public static String distance(double km) {
        return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US)).format(km);
    }

    /** 作物标签「黄瓜、白菜」→「黄瓜 · 白菜」（D01 本季高发卡片） */
    @NonNull
    public static String cropsDot(@Nullable String tags) {
        if (tags == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String t : tags.split("[、,，]")) {
            String s = t.trim();
            if (s.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(s);
        }
        return sb.toString();
    }
}
