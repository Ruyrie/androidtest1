package com.heban.app.ui.account;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.google.android.material.textfield.TextInputLayout;
import com.heban.app.R;

/** A07 / A08 / A09 共用的界面小工具：「+86 |」前缀、框内「获取验证码」、按钮加载态、密码规则行。 */
final class ResetUi {
    private ResetUi() {
    }

    static int dp(@NonNull Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static int color(@NonNull View v, @AttrRes int attr) {
        return MaterialColors.getColor(v, attr);
    }

    /** 「+86」后面加 1dp × 24dp 的竖线（Outline Variant），左右各留 8dp */
    static void decoratePhonePrefix(@NonNull TextInputLayout til) {
        Context c = til.getContext();
        Drawable line = new PrefixDivider(color(til, com.google.android.material.R.attr.colorOutlineVariant),
                dp(c, 1), dp(c, 24), dp(c, 6));
        TextView prefix = til.getPrefixTextView();
        prefix.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, line, null);
        prefix.setCompoundDrawablePadding(dp(c, 8));
    }

    /**
     * 前缀后的竖线。固有高度为 0，不会撑高输入框（保持 56dp）；竖线以文字为中心向上下各画 12dp。
     * 固有宽度包含竖线之后的留白（输入框自带约 2dp，合计 8dp）。
     */
    private static final class PrefixDivider extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int width;
        private final int height;
        private final int gapAfter;

        PrefixDivider(int color, int width, int height, int gapAfter) {
            paint.setColor(color);
            this.width = width;
            this.height = height;
            this.gapAfter = gapAfter;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            float cy = b.exactCenterY();
            canvas.drawRect(b.left, cy - height / 2f, b.left + width, cy + height / 2f, paint);
        }

        @Override
        public int getIntrinsicWidth() {
            return width + gapAfter;
        }

        @Override
        public int getIntrinsicHeight() {
            return 0;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * 把「竖线 + 获取验证码」放进输入框的边框内，靠右垂直居中。
     * action 在布局里先作为普通子 View 声明（DataBinding 照常绑定），这里移到输入框内部。
     */
    static void attachInlineAction(@NonNull TextInputLayout til, @NonNull View action) {
        EditText et = til.getEditText();
        if (et == null || !(et.getParent() instanceof FrameLayout)) return;
        FrameLayout inputFrame = (FrameLayout) et.getParent();
        if (action.getParent() == inputFrame) return;
        if (action.getParent() instanceof ViewGroup) ((ViewGroup) action.getParent()).removeView(action);
        inputFrame.addView(action, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL));
    }

    /** 主按钮加载中：文字前显示小号环形进度，并且不可再次点击 */
    static void setLoading(@NonNull MaterialButton button, boolean loading) {
        if (loading) {
            Context c = button.getContext();
            CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(c, null, 0,
                    com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
            spec.indicatorColors = new int[]{button.getCurrentTextColor()};
            IndeterminateDrawable<CircularProgressIndicatorSpec> d = IndeterminateDrawable.createCircularDrawable(c, spec);
            button.setIcon(d);
            button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            button.setIconTint(null);
        } else {
            button.setIcon(null);
        }
        button.setClickable(!loading);
    }

    /** 密码规则行：满足 = 实心 check_circle（Primary）；未满足 / 待校验 = radio_button_unchecked（Outline）；被拒绝 = error（Error） */
    static void applyRule(@NonNull ImageView icon, @NonNull TextView text, ResetPwdViewModel.Rule rule) {
        int iconRes;
        int iconAttr;
        int textAttr = com.google.android.material.R.attr.colorOnSurface;
        int stateRes;
        if (rule == ResetPwdViewModel.Rule.MET) {
            iconRes = R.drawable.ic_check_circle_fill;
            iconAttr = androidx.appcompat.R.attr.colorPrimary;
            stateRes = R.string.reset_rule_met;
        } else if (rule == ResetPwdViewModel.Rule.FAILED) {
            iconRes = R.drawable.ic_error_fill;
            iconAttr = androidx.appcompat.R.attr.colorError;
            textAttr = androidx.appcompat.R.attr.colorError;
            stateRes = R.string.reset_rule_unmet;
        } else {
            iconRes = R.drawable.ic_radio_button_unchecked;
            iconAttr = com.google.android.material.R.attr.colorOutline;
            stateRes = rule == ResetPwdViewModel.Rule.PENDING ? R.string.reset_rule_unknown : R.string.reset_rule_unmet;
        }
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(color(icon, iconAttr)));
        text.setTextColor(color(text, textAttr));
        text.setContentDescription(text.getText() + "，" + text.getContext().getString(stateRes));
    }

    /** 手机号按 3-4-4 加空格显示（138 0000 5678），光标保持在原来的数字之后 */
    static final class PhoneSpacingWatcher implements TextWatcher {
        private boolean selfChange;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (selfChange) return;
            String raw = s.toString();
            int cursor = Selection.getSelectionEnd(s);
            int digitsBeforeCursor = 0;
            for (int i = 0; i < Math.min(cursor, raw.length()); i++) {
                if (Character.isDigit(raw.charAt(i))) digitsBeforeCursor++;
            }
            String formatted = format(ResetPwdViewModel.digits(raw));
            if (formatted.equals(raw)) return;
            selfChange = true;
            s.replace(0, s.length(), formatted);
            selfChange = false;
            int pos = 0;
            int seen = 0;
            while (pos < formatted.length() && seen < digitsBeforeCursor) {
                if (Character.isDigit(formatted.charAt(pos))) seen++;
                pos++;
            }
            Selection.setSelection(s, cursor < 0 ? formatted.length() : pos);
        }

        static String format(String digits) {
            String d = digits.length() > 11 ? digits.substring(0, 11) : digits;
            if (d.length() <= 3) return d;
            if (d.length() <= 7) return d.substring(0, 3) + " " + d.substring(3);
            return d.substring(0, 3) + " " + d.substring(3, 7) + " " + d.substring(7);
        }
    }
}
