package com.heban.app.ui.common.image;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.squareup.picasso.Transformation;

/** Picasso 圆角变换（技术准备文档 10.2）：详情页大图配合 fit().centerCrop() 使用。 */
public final class RoundedCornersTransformation implements Transformation {
    private final float radius;

    public RoundedCornersTransformation(float radiusPx) {
        this.radius = radiusPx;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        int w = source.getWidth(), h = source.getHeight();
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        new Canvas(out).drawRoundRect(new RectF(0, 0, w, h), radius, radius, paint);
        if (out != source) source.recycle();             // Picasso 要求：返回新图时回收原图
        return out;
    }

    @Override
    public String key() {
        return "rounded-" + radius;                      // 参与缓存键
    }
}
