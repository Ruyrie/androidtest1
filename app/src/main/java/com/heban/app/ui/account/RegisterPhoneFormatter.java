package com.heban.app.ui.account;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;

/**
 * 手机号输入时按 3-4-4 分组显示（138 0000 5678，与原型一致），只保留数字，最多 11 位。
 * 光标保持在原来的数字之后。
 */
final class RegisterPhoneFormatter implements TextWatcher {
    private static final int MAX_DIGITS = 11;
    private boolean selfChange;

    /** "13800005678" → "138 0000 5678" */
    static String format(CharSequence raw) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length() && digits.length() < MAX_DIGITS; i++) {
            char ch = raw.charAt(i);
            if (ch >= '0' && ch <= '9') digits.append(ch);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i == 3 || i == 7) out.append(' ');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (selfChange) return;
        String formatted = format(s);
        if (formatted.contentEquals(s)) return;

        // 记录光标前的数字个数，格式化后把光标放回同一个数字之后
        int cursor = Selection.getSelectionEnd(s);
        int digitsBefore = 0;
        for (int i = 0; i < Math.max(0, Math.min(cursor, s.length())); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') digitsBefore++;
        }
        selfChange = true;
        s.replace(0, s.length(), formatted);
        selfChange = false;

        int pos = 0;
        int seen = 0;
        while (pos < formatted.length() && seen < digitsBefore) {
            if (formatted.charAt(pos) != ' ') seen++;
            pos++;
        }
        Selection.setSelection(s, Math.min(pos, s.length()));
    }
}
