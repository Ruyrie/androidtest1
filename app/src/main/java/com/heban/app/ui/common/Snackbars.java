package com.heban.app.ui.common;

import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

/** 统一的 Snackbar：LENGTH_LONG，最多一个操作（UI 设计文档 6 章）。 */
public final class Snackbars {
    private Snackbars() {
    }

    public static void show(@NonNull View anchor, @NonNull CharSequence text) {
        Snackbar.make(anchor, text, Snackbar.LENGTH_LONG).show();
    }

    public static void show(@NonNull View anchor, @NonNull CharSequence text,
                            @NonNull CharSequence action, @NonNull View.OnClickListener onAction) {
        Snackbar.make(anchor, text, Snackbar.LENGTH_LONG).setAction(action, onAction).show();
    }
}
