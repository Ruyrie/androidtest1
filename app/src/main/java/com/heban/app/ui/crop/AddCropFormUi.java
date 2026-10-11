package com.heban.app.ui.crop;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.heban.app.R;

import java.time.LocalDate;

/** C03 / C04 表单共用的界面小工具：日期文字、开关勾选图标、输入框错误、保存后返回并提示。 */
final class AddCropFormUi {
    private AddCropFormUi() {
    }

    /** 2026年10月10日 */
    static String date(@NonNull Context context, @NonNull LocalDate d) {
        return context.getString(R.string.add_crop_date, d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    /** 开关打开时滑块内显示勾；关闭时不放图标，滑块保持 16dp（原型 .sw） */
    static void renderSwitchIcon(@NonNull MaterialSwitch sw, boolean checked) {
        sw.setThumbIconDrawable(checked ? ContextCompat.getDrawable(sw.getContext(), R.drawable.ic_check) : null);
    }

    /** 输入框下方的错误：有错误时显示，消失后收起占位 */
    static void renderError(@NonNull TextInputLayout til, @Nullable CharSequence error) {
        boolean on = error != null && error.length() > 0;
        til.setError(on ? error : null);
        til.setErrorEnabled(on);
    }

    /** 生长期表中的 Material Symbol 名称 → @drawable/ic_&lt;name&gt;，找不到时用 eco */
    @DrawableRes
    static int cropIcon(@NonNull Context context, @Nullable String symbol) {
        if (symbol != null && !symbol.isEmpty()) {
            int id = context.getResources().getIdentifier("ic_" + symbol, "drawable", context.getPackageName());
            if (id != 0) return id;
        }
        return R.drawable.ic_eco;
    }

    static void hideKeyboard(@NonNull View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        View focus = view.findFocus();
        if (focus != null) focus.clearFocus();
    }

    /**
     * 保存成功：返回上一页，并在上一页显示 Snackbar。
     * Snackbar 挂在 Activity 的内容区上，页面切换后仍然显示；上一页有「问 AI」按钮或底部导航时显示在其上方。
     */
    static void finishWithMessage(@NonNull Activity activity, @NonNull NavController nav, @NonNull CharSequence text) {
        View content = activity.findViewById(android.R.id.content);
        hideKeyboard(content);
        nav.navigateUp();
        content.post(() -> {
            Snackbar bar = Snackbar.make(content, text, Snackbar.LENGTH_LONG);
            View fab = activity.findViewById(R.id.fab_ai);
            View bottomNav = activity.findViewById(R.id.bottom_nav);
            if (fab != null && fab.getVisibility() == View.VISIBLE) bar.setAnchorView(fab);
            else if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) bar.setAnchorView(bottomNav);
            bar.show();
        });
    }
}
