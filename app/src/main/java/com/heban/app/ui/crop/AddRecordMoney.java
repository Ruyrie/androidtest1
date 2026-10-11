package com.heban.app.ui.crop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 金额输入（元）与「分」互转：记账金额统一存分（技术准备文档表 7-1）。 */
public final class AddRecordMoney {
    /** 输入不合法（多于两位小数、格式错误、超出上限） */
    public static final long INVALID = -1;
    /** 最多 8 位整数（99,999,999.99 元） */
    private static final int MAX_INTEGER_DIGITS = 8;

    private AddRecordMoney() {
    }

    /** 「60」→ 6000，「60.5」→ 6050，「.5」→ 50，空白 → 0；不合法返回 {@link #INVALID} */
    public static long parseCents(@Nullable String text) {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) return 0;
        if (!s.matches("\\d*(\\.\\d*)?") || s.equals(".")) return INVALID;
        int dot = s.indexOf('.');
        String intPart = dot < 0 ? s : s.substring(0, dot);
        String fracPart = dot < 0 ? "" : s.substring(dot + 1);
        if (fracPart.length() > 2) return INVALID;
        intPart = stripLeadingZeros(intPart);
        if (intPart.length() > MAX_INTEGER_DIGITS) return INVALID;
        long yuan = intPart.isEmpty() ? 0 : Long.parseLong(intPart);
        long cents = fracPart.isEmpty() ? 0
                : Long.parseLong(fracPart.length() == 1 ? fracPart + "0" : fracPart);
        return yuan * 100 + cents;
    }

    /** 「1.234」这类超过两位小数的输入 */
    public static boolean hasTooManyDecimals(@Nullable String text) {
        return text != null && text.trim().matches("\\d*\\.\\d{3,}");
    }

    /** 显示金额：整数元不带小数（6000 → 60），否则两位小数（6050 → 60.50） */
    @NonNull
    public static String formatYuan(long cents) {
        long abs = Math.abs(cents);
        String sign = cents < 0 ? "-" : "";
        long yuan = abs / 100;
        long rest = abs % 100;
        if (rest == 0) return sign + yuan;
        return sign + yuan + "." + (rest < 10 ? "0" : "") + rest;
    }

    private static String stripLeadingZeros(String s) {
        int i = 0;
        while (i < s.length() - 1 && s.charAt(i) == '0') i++;
        return s.substring(i);
    }
}
