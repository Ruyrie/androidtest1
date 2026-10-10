package com.heban.app.util;

import android.net.Uri;

import androidx.annotation.Nullable;

/** 统一处理图片路径：assets 中的相对路径转为 file:///android_asset/（技术准备文档 6.2）。 */
public final class ImagePaths {
    private static final String ASSET_PREFIX = "file:///android_asset/";

    private ImagePaths() {
    }

    /** "images/crops/tomato.jpg" → assets；http / file / content 地址原样返回 */
    @Nullable
    public static Uri toUri(@Nullable String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("http") || path.startsWith("file:") || path.startsWith("content:")) {
            return Uri.parse(path);
        }
        return Uri.parse(ASSET_PREFIX + path);
    }
}
