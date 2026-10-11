package com.heban.app.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

/** H02 未来 7 天温度条：高 4dp 圆角轨道（Surface Container Highest），按一周最低 / 最高温画出当天区间。 */
public class WeatherRangeBar extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rangePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float left;
    private float width = 1f;

    public WeatherRangeBar(@NonNull Context context) {
        this(context, null);
    }

    public WeatherRangeBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        trackPaint.setColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainerHighest));
        rangePaint.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** left、width 为 0–1 的比例 */
    public void setRange(float left, float width, @ColorInt int color) {
        this.left = left;
        this.width = width;
        rangePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float w = getWidth() - getPaddingLeft() - getPaddingRight();
        float h = getHeight();
        float r = h / 2f;
        float x0 = getPaddingLeft();
        rect.set(x0, 0, x0 + w, h);
        canvas.drawRoundRect(rect, r, r, trackPaint);
        if (width <= 0f) return;
        float start = x0 + w * left;
        float end = Math.max(start + h, x0 + w * (left + width));
        rect.set(start, 0, Math.min(end, x0 + w), h);
        canvas.drawRoundRect(rect, r, r, rangePaint);
    }
}
