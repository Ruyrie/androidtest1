package com.heban.app.ui.discover.ledger;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** D11 数字键盘规则：与原型 AddEntry 的 press / back / clear 逐一对照。 */
public class AmountInputTest {
    private static final List<String> SINGLE_KEYS =
            Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".");

    /** 原型 JS 的按键逻辑（逐行翻译），只用于单个字符的按键 */
    private static String prototypePress(String amt, String s) {
        String a = amt.equals("0") ? "" : amt;
        if (s.equals(".") && (a.contains(".") || a.isEmpty())) {
            if (a.isEmpty()) a = "0";
            else return amt;
        }
        int dot = a.indexOf('.');
        if (dot >= 0 && !s.equals(".") && a.length() - dot > 2) return amt;
        if (a.replace(".", "").length() >= 8) return amt;
        return a + s;
    }

    private static String prototypeBack(String amt) {
        return amt.length() > 1 ? amt.substring(0, amt.length() - 1) : "0";
    }

    private static String type(String keys) {
        String a = AmountInput.ZERO;
        for (char ch : keys.toCharArray()) a = AmountInput.press(a, String.valueOf(ch));
        return a;
    }

    @Test
    public void digitsReplaceLeadingZero() {
        assertEquals("6", AmountInput.press("0", "6"));
        assertEquals("60", type("60"));
        assertEquals("0", AmountInput.press("0", "0"));
        assertEquals("0", type("000"));
        assertEquals("5", type("005"));
    }

    @Test
    public void doubleZero() {
        assertEquals("0", AmountInput.press("0", "00"));
        assertEquals("600", AmountInput.press("6", "00"));
        assertEquals("6.00", AmountInput.press("6.", "00"));
        // 只剩一位小数的名额：只补一个 0
        assertEquals("6.50", AmountInput.press("6.5", "00"));
        assertEquals("6.55", AmountInput.press("6.55", "00"));
        // 只剩一位数字的名额：只补一个 0
        assertEquals("12345670", AmountInput.press("1234567", "00"));
        assertEquals("12345678", AmountInput.press("12345678", "00"));
    }

    @Test
    public void singleDot() {
        assertEquals("0.", AmountInput.press("0", "."));
        assertEquals("6.", AmountInput.press("6", "."));
        assertEquals("6.", AmountInput.press("6.", "."));
        assertEquals("6.5", AmountInput.press("6.5", "."));
        assertEquals("0.05", type(".05"));
    }

    @Test
    public void atMostTwoDecimals() {
        assertEquals("1.23", type("1.234"));
        assertEquals("60.5", type("60.5"));
        assertEquals("0.99", type("0.999"));
    }

    @Test
    public void atMostEightDigits() {
        assertEquals("12345678", type("123456789"));
        // 已满 8 位时也不能再输入小数点
        assertEquals("12345678", type("12345678."));
        assertEquals("1234567.8", type("1234567.89"));
        assertEquals("123456.78", type("123456.789"));
    }

    @Test
    public void backspaceAndClear() {
        assertEquals("0", AmountInput.backspace("6"));
        assertEquals("6", AmountInput.backspace("60"));
        assertEquals("1.", AmountInput.backspace("1.2"));
        assertEquals("0", AmountInput.backspace("0."));
        assertEquals("0", AmountInput.backspace("0"));
        assertEquals("0", AmountInput.clear());
    }

    @Test
    public void invalidInputTreatedAsZero() {
        assertEquals("5", AmountInput.press(null, "5"));
        assertEquals("5", AmountInput.press("", "5"));
        assertEquals("5", AmountInput.press("abc", "5"));
        assertEquals("0", AmountInput.press("0", "x"));
        assertEquals("0", AmountInput.backspace(null));
    }

    @Test
    public void cents() {
        assertEquals(6000, AmountInput.toCents("60"));
        assertEquals(6050, AmountInput.toCents("60.5"));
        assertEquals(6005, AmountInput.toCents("60.05"));
        assertEquals(100, AmountInput.toCents("1."));
        assertEquals(0, AmountInput.toCents("0."));
        assertEquals(0, AmountInput.toCents("0"));
        assertEquals(0, AmountInput.toCents(null));
        assertEquals(1234567800L, AmountInput.toCents("12345678"));
        assertEquals(12345678L, AmountInput.toCents("123456.78"));
    }

    @Test
    public void fromCents() {
        assertEquals("60", AmountInput.fromCents(6000));
        assertEquals("60.5", AmountInput.fromCents(6050));
        assertEquals("60.05", AmountInput.fromCents(6005));
        assertEquals("0.01", AmountInput.fromCents(1));
        assertEquals("0", AmountInput.fromCents(0));
        assertEquals("0", AmountInput.fromCents(-5));
        for (long c : new long[]{1, 9, 10, 99, 100, 101, 110, 6050, 6005, 99999999L, 1234567899L}) {
            assertEquals(c, AmountInput.toCents(AmountInput.fromCents(c)));
        }
    }

    /**
     * 穷举：从「0」出发按 4 次键（数字、小数点、退格、清空）能到达的所有状态，以及接近上限的状态，
     * 每个单字符按键与退格的结果都与原型一致；「00」等于连按两次「0」。
     */
    @Test
    public void exhaustiveAgainstPrototype() {
        Set<String> seen = new HashSet<>();
        Deque<String[]> queue = new ArrayDeque<>();
        for (String start : new String[]{"0", "1234567", "12345678", "123456.7", "1234567.", "0.0", "9.9", "99.99"}) {
            queue.add(new String[]{start, "0"});
        }
        int checked = 0;
        while (!queue.isEmpty()) {
            String[] cur = queue.poll();
            String state = cur[0];
            int depth = Integer.parseInt(cur[1]);
            if (!seen.add(state + "@" + depth)) continue;
            for (String k : SINGLE_KEYS) {
                String expected = prototypePress(state, k);
                assertEquals("press " + k + " on " + state, expected, AmountInput.press(state, k));
                checked++;
                if (depth < 4) queue.add(new String[]{expected, String.valueOf(depth + 1)});
            }
            assertEquals("00 on " + state, AmountInput.press(AmountInput.press(state, "0"), "0"),
                    AmountInput.press(state, "00"));
            String back = prototypeBack(state);
            assertEquals("back on " + state, back, AmountInput.backspace(state));
            if (depth < 4) queue.add(new String[]{back, String.valueOf(depth + 1)});
            checked++;
        }
        // 确保确实覆盖了大量状态
        assertEquals(true, checked > 10000);
    }
}
