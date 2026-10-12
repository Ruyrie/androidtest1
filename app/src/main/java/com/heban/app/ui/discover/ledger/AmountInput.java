package com.heban.app.ui.discover.ledger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * D11 自定义数字键盘维护的金额字符串（技术准备文档 12.4，规则同原型 AddEntry 的按键逻辑）：
 * <ul>
 *     <li>金额为「0」时按数字直接替换（不出现前导 0），按「0」「00」保持为「0」；</li>
 *     <li>小数点只能有一个，金额为 0 时按小数点得到「0.」；</li>
 *     <li>最多两位小数；</li>
 *     <li>最多 8 位数字（整数和小数合计，不含小数点），已满 8 位时不能再输入数字或小数点；</li>
 *     <li>「00」相当于连按两次「0」，每一次都受以上规则限制；</li>
 *     <li>退格删到只剩一位时变回「0」，清空变回「0」。</li>
 * </ul>
 * 纯 Java，便于单元测试。
 */
public final class AmountInput {
    public static final String ZERO = "0";
    public static final int MAX_DIGITS = 8;
    public static final int MAX_DECIMALS = 2;

    public static final String KEY_DOT = ".";
    public static final String KEY_DOUBLE_ZERO = "00";

    private AmountInput() {
    }

    /** 按下一个键（「0」–「9」、「.」、「00」），返回新的金额字符串；不允许的输入返回原字符串 */
    @NonNull
    public static String press(@Nullable String current, @NonNull String key) {
        String amt = normalize(current);
        if (KEY_DOUBLE_ZERO.equals(key)) return press(press(amt, "0"), "0");
        if (!isSingleKey(key)) return amt;
        String a = ZERO.equals(amt) ? "" : amt;
        if (KEY_DOT.equals(key) && (a.contains(KEY_DOT) || a.isEmpty())) {
            if (a.isEmpty()) a = ZERO;
            else return amt;
        }
        int dot = a.indexOf('.');
        if (dot >= 0 && !KEY_DOT.equals(key) && a.length() - dot > MAX_DECIMALS) return amt;
        if (a.replace(KEY_DOT, "").length() >= MAX_DIGITS) return amt;
        // 金额为 0 时按「0」：a 为空，结果仍是「0」
        return a + key;
    }

    /** 退格：只剩一位时变回「0」 */
    @NonNull
    public static String backspace(@Nullable String current) {
        String amt = normalize(current);
        return amt.length() > 1 ? amt.substring(0, amt.length() - 1) : ZERO;
    }

    /** 清空 */
    @NonNull
    public static String clear() {
        return ZERO;
    }

    /** 金额字符串 → 分：「60」→ 6000，「60.5」→ 6050，「0.」→ 0，「1.05」→ 105 */
    public static long toCents(@Nullable String amount) {
        String s = normalize(amount);
        int dot = s.indexOf('.');
        String intPart = dot < 0 ? s : s.substring(0, dot);
        String frac = dot < 0 ? "" : s.substring(dot + 1);
        if (frac.length() > MAX_DECIMALS) frac = frac.substring(0, MAX_DECIMALS);
        long yuan = intPart.isEmpty() ? 0 : Long.parseLong(intPart);
        long cents = frac.isEmpty() ? 0 : Long.parseLong(frac.length() == 1 ? frac + "0" : frac);
        return yuan * 100 + cents;
    }

    /** 分 → 金额字符串（编辑时预填）：6000 →「60」，6050 →「60.5」，6005 →「60.05」，0 或负数 →「0」 */
    @NonNull
    public static String fromCents(long cents) {
        if (cents <= 0) return ZERO;
        long yuan = cents / 100;
        long rest = cents % 100;
        if (rest == 0) return String.valueOf(yuan);
        if (rest % 10 == 0) return yuan + "." + (rest / 10);
        return yuan + "." + (rest < 10 ? "0" : "") + rest;
    }

    /** 只接受由数字和至多一个小数点组成的字符串，其余（含 null、空串）视为「0」 */
    @NonNull
    private static String normalize(@Nullable String s) {
        if (s == null || s.isEmpty() || !s.matches("\\d+(\\.\\d*)?")) return ZERO;
        return s;
    }

    private static boolean isSingleKey(String key) {
        return KEY_DOT.equals(key) || (key.length() == 1 && key.charAt(0) >= '0' && key.charAt(0) <= '9');
    }
}
