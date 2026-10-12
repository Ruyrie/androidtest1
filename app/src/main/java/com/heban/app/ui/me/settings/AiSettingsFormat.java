package com.heban.app.ui.me.settings;

import androidx.annotation.NonNull;

import java.util.Locale;

/** M11「12 个对话 · 占用 2.4 MB」中的占用空间。 */
public final class AiSettingsFormat {
    private static final long KB = 1024;
    private static final long MB = 1024 * 1024;

    private AiSettingsFormat() {
    }

    /** 不足 1 MB 显示 KB（向上取整），否则保留一位小数的 MB */
    @NonNull
    public static String size(long bytes) {
        if (bytes <= 0) return "0 KB";
        if (bytes < MB) return String.format(Locale.ROOT, "%d KB", Math.max(1, (bytes + KB - 1) / KB));
        return String.format(Locale.ROOT, "%.1f MB", bytes / (double) MB);
    }
}
