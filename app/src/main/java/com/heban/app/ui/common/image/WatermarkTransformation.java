package com.heban.app.ui.common.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

import com.squareup.picasso.Transformation;

/**
 * Picasso 水印变换（技术准备文档 10.2）：在图片右下角绘制「禾伴 @昵称」，用于 D08 商品详情轮播。
 * 白色半透明文字 + 阴影，深浅背景都能看清。可与其他变换链式调用：.transform(a).transform(b)。
 */
public final class WatermarkTransformation implements Transformation {
    private final String text;
    private final float textSizePx;
    private final float marginEndPx;
    private final float marginBottomPx;

    /**
     * @param text           水印文字，如「禾伴 @草莓姐姐」
     * @param textSizePx     文字大小（像素，按控件尺寸 fit() 后的图片计算）
     * @param marginEndPx    距右边的距离（像素）
     * @param marginBottomPx 文字底部距下边的距离（像素）
     */
    public WatermarkTransformation(@NonNull String text, float textSizePx, float marginEndPx, float marginBottomPx) {
        this.text = text;
        this.textSizePx = textSizePx;
        this.marginEndPx = marginEndPx;
        this.marginBottomPx = marginBottomPx;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap out = source.isMutable() ? source : source.copy(Bitmap.Config.ARGB_8888, true);
        if (out == null) return source;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(0xE6, 0xFF, 0xFF, 0xFF));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        // 图片很小时按宽度缩小文字，避免水印超出画面
        float size = Math.min(textSizePx, out.getWidth() / 12f);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setShadowLayer(size / 4f, 0, size / 12f, Color.argb(0x66, 0, 0, 0));
        float end = Math.min(marginEndPx, out.getWidth() / 16f);
        float bottom = Math.min(marginBottomPx, out.getHeight() / 4f);
        new Canvas(out).drawText(text, out.getWidth() - end, out.getHeight() - bottom - paint.descent(), paint);
        if (out != source) source.recycle();            // Picasso 要求：返回新图时回收原图
        return out;
    }

    @Override
    public String key() {
        return "watermark-" + text + "-" + textSizePx + "-" + marginEndPx + "-" + marginBottomPx;   // 参与缓存键
    }
}
