package com.heban.app.ui.discover.scan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.io.IOException;

/**
 * 识别用照片的获取（技术准备文档 9.5）：拍照结果按旋转角度转正，相册图片用 ImageDecoder 软件解码；
 * 长边限制在 {@link #MAX_EDGE} 以内（上传前仓库还会压缩到 1280px），避免整张大图常驻内存。
 */
final class ScanImages {
    static final int MAX_EDGE = 1600;

    private ScanImages() {
    }

    /** 拍照结果 → 转正、缩小后的 Bitmap；调用方负责关闭 image */
    @NonNull
    static Bitmap upright(@NonNull ImageProxy image) {
        Bitmap bitmap = image.toBitmap();
        int rotation = image.getImageInfo().getRotationDegrees();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float scale = Math.min(1f, MAX_EDGE / (float) Math.max(w, h));
        if (rotation == 0 && scale >= 1f) return bitmap;
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        m.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, m, true);
    }

    /** 系统照片选择器返回的图片（ImageDecoder + ALLOCATOR_SOFTWARE，自动按 EXIF 转正） */
    @NonNull
    static Bitmap decode(@NonNull Context context, @NonNull Uri uri) throws IOException {
        ImageDecoder.Source src = ImageDecoder.createSource(context.getContentResolver(), uri);
        return ImageDecoder.decodeBitmap(src, (decoder, info, source) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            Size s = info.getSize();
            int max = Math.max(s.getWidth(), s.getHeight());
            if (max > MAX_EDGE) {
                float scale = MAX_EDGE / (float) max;
                decoder.setTargetSize(Math.max(1, Math.round(s.getWidth() * scale)),
                        Math.max(1, Math.round(s.getHeight() * scale)));
            }
        });
    }
}
