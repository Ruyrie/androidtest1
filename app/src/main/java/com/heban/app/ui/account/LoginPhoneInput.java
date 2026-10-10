package com.heban.app.ui.account;

import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.textfield.TextInputLayout;
import com.heban.app.R;

/**
 * A03 / A04 的手机号输入框：「+86」前缀后补上下拉箭头和分隔线，输入内容按 3-4-4 加空格（138 0000 5678）。
 * ViewModel 中用 {@link #digits(String)} 取出纯数字。
 */
final class LoginPhoneInput {
    static final int PHONE_LENGTH = 11;

    private LoginPhoneInput() {
    }

    static void install(TextInputLayout til, EditText editText) {
        Drawable end = AppCompatResources.getDrawable(til.getContext(), R.drawable.login_phone_prefix_end);
        til.getPrefixTextView().setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, end, null);
        editText.addTextChangedListener(new SpacingWatcher());
    }

    /** 去掉空格等非数字字符 */
    static String digits(@Nullable String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

    /** 13800005678 → 138 0000 5678（不足 11 位时按已输入的部分分隔） */
    static String format(@Nullable String text) {
        String d = digits(text);
        if (d.length() > PHONE_LENGTH) d = d.substring(0, PHONE_LENGTH);
        if (d.length() <= 3) return d;
        if (d.length() <= 7) return d.substring(0, 3) + " " + d.substring(3);
        return d.substring(0, 3) + " " + d.substring(3, 7) + " " + d.substring(7);
    }

    /** 输入时自动加空格；退格删到空格时连同前一位数字一起删除，光标停在原来的数字位置 */
    static final class SpacingWatcher implements TextWatcher {
        private boolean selfChange;
        private int deletedSpaceAt = -1;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (selfChange) return;
            deletedSpaceAt = count == 1 && after == 0 && s.charAt(start) == ' ' ? start : -1;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (selfChange) return;
            String text = s.toString();
            int cursor = Selection.getSelectionEnd(s);
            if (cursor < 0 || cursor > text.length()) cursor = text.length();
            if (deletedSpaceAt > 0 && deletedSpaceAt <= text.length()) {
                text = text.substring(0, deletedSpaceAt - 1) + text.substring(deletedSpaceAt);
                cursor = deletedSpaceAt - 1;
            }
            deletedSpaceAt = -1;
            int digitsBeforeCursor = digits(text.substring(0, cursor)).length();
            String formatted = format(text);
            if (formatted.equals(s.toString())) return;
            selfChange = true;
            s.replace(0, s.length(), formatted);
            selfChange = false;
            Selection.setSelection(s, positionAfterDigits(formatted, digitsBeforeCursor));
        }

        private static int positionAfterDigits(String formatted, int digitCount) {
            if (digitCount <= 0) return 0;
            int seen = 0;
            for (int i = 0; i < formatted.length(); i++) {
                if (Character.isDigit(formatted.charAt(i)) && ++seen == digitCount) return i + 1;
            }
            return formatted.length();
        }
    }
}
