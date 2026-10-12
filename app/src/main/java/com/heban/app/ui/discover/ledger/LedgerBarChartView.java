package com.heban.app.ui.discover.ledger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D10 近 6 个月结余柱状图（技术准备文档 12.4：自定义 View，Canvas 绘制；原型 Ledger「结余趋势」）。
 * <ul>
 *     <li>每列等宽，列间距 12dp，左右内边距 4dp；柱宽 28dp，顶部圆角 4dp；</li>
 *     <li>当前月（最后一根）和点选的柱子用 Primary，其余用由 Primary 推导的浅色；</li>
 *     <li>数值标签只显示在选中的柱子上（默认当前月），点同一根柱子取消选中；</li>
 *     <li>负结余画在 0 线以下，颜色用 Error；</li>
 *     <li>标签用文字色（On Surface / On Surface Variant），不用系列色。</li>
 * </ul>
 * 所有颜色取自主题，深色模式自动适配。
 */
public class LedgerBarChartView extends View {
    private static final float DEFAULT_HEIGHT_DP = 176f;
    private static final float PADDING_H_DP = 4f;
    private static final float GAP_DP = 12f;
    private static final float BAR_WIDTH_DP = 28f;
    private static final float CORNER_DP = 4f;
    private static final float TEXT_LINE_DP = 16f;
    private static final float TEXT_GAP_DP = 6f;
    /** 月份行：上边距 6dp + 行高 16dp */
    private static final float MONTH_ROW_DP = 22f;

    private final float density;
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint();
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint monthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final float[] radiiTop;
    private final float[] radiiBottom;

    @ColorInt private int colorPrimary;
    @ColorInt private int colorPrimaryLight;
    @ColorInt private int colorError;
    @ColorInt private int colorErrorLight;

    private List<LedgerChartModel.Bar> bars = Collections.emptyList();
    /** 点选的柱子；-1 表示默认（当前月） */
    private int selected = -1;
    @Nullable private OnBarSelectedListener listener;

    /** 柱子被点选（index 为 -1 表示回到默认） */
    public interface OnBarSelectedListener {
        void onBarSelected(int index);
    }

    public LedgerBarChartView(@NonNull Context context) {
        this(context, null);
    }

    public LedgerBarChartView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        float r = CORNER_DP * density;
        radiiTop = new float[]{r, r, r, r, 0, 0, 0, 0};
        radiiBottom = new float[]{0, 0, 0, 0, r, r, r, r};

        int onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        int onSurfaceVariant = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        int surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface);
        colorPrimary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary);
        colorError = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorError);
        colorPrimaryLight = lighterTint(colorPrimary, surface);
        colorErrorLight = lighterTint(colorError, surface);

        axisPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant));
        axisPaint.setStyle(Paint.Style.FILL);

        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());
        valuePaint.setTextSize(textSize);
        valuePaint.setColor(onSurface);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, 500, false));
        monthPaint.setTextSize(textSize);
        monthPaint.setColor(onSurfaceVariant);
        monthPaint.setTextAlign(Paint.Align.CENTER);

        setClickable(true);
        setFocusable(true);
    }

    /**
     * 「其余月份」的浅色：在 HSL 中把亮度向 Surface 靠近 30%、饱和度降为 70%。
     * 浅色主题由 #456732 得到约 #769E60（原型 #759B5F）；深色主题同样由主题色推导。
     */
    @ColorInt
    public static int lighterTint(@ColorInt int color, @ColorInt int surface) {
        float[] hsl = new float[3];
        float[] surfaceHsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        ColorUtils.colorToHSL(surface, surfaceHsl);
        hsl[1] = hsl[1] * 0.7f;
        hsl[2] = hsl[2] + (surfaceHsl[2] - hsl[2]) * 0.3f;
        return ColorUtils.HSLToColor(hsl);
    }

    public void setOnBarSelectedListener(@Nullable OnBarSelectedListener l) {
        listener = l;
    }

    /** 更新数据；月份变化（切换月份）时清除点选 */
    public void setBars(@Nullable List<LedgerChartModel.Bar> list) {
        List<LedgerChartModel.Bar> next = list == null ? Collections.emptyList() : new ArrayList<>(list);
        if (!sameMonths(bars, next)) selected = -1;
        bars = next;
        setContentDescription(next.isEmpty() ? null : LedgerChartModel.describe(next));
        invalidate();
    }

    /** 当前显示数值标签的柱子（默认最后一根） */
    public int highlightedIndex() {
        if (bars.isEmpty()) return -1;
        return selected >= 0 && selected < bars.size() ? selected : bars.size() - 1;
    }

    /** 点选一根柱子；再次点选同一根时回到默认（当前月） */
    public void select(int index) {
        if (index < 0 || index >= bars.size()) return;
        selected = (index == selected || index == bars.size() - 1) ? -1 : index;
        invalidate();
        if (listener != null) listener.onBarSelected(selected);
    }

    private static boolean sameMonths(List<LedgerChartModel.Bar> a, List<LedgerChartModel.Bar> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).label.equals(b.get(i).label)) return false;
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = resolveSize(Math.round(DEFAULT_HEIGHT_DP * density), heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int n = bars.size();
        float width = getWidth();
        float areaH = getHeight() - MONTH_ROW_DP * density;
        if (n == 0 || areaH <= 0) return;

        long[] values = new long[n];
        for (int i = 0; i < n; i++) values[i] = bars.get(i).cents;
        LedgerChartModel.BarLayout layout = LedgerChartModel.layout(values, areaH / density);
        float baseline = layout.baseline * density;
        float axis = Math.max(1f, density);

        float padH = PADDING_H_DP * density;
        float gap = GAP_DP * density;
        float colW = (width - 2 * padH - gap * (n - 1)) / n;
        float barW = Math.min(BAR_WIDTH_DP * density, colW);
        int highlight = highlightedIndex();

        // 0 线（只有正值时即图区底边，原型 border-bottom 1px Outline Variant）
        if (layout.hasNegative) {
            canvas.drawRect(0, baseline, width, baseline + axis, axisPaint);
        } else {
            canvas.drawRect(0, areaH - axis, width, areaH, axisPaint);
        }

        for (int i = 0; i < n; i++) {
            float cx = padH + i * (colW + gap) + colW / 2f;
            long v = values[i];
            boolean strong = i == n - 1 || i == highlight;
            float top = layout.top[i] * density;
            float bottom = layout.bottom[i] * density;
            if (!layout.hasNegative) {
                // 柱子落在底边线上方
                top -= axis;
                bottom -= axis;
            } else if (v < 0) {
                top += axis;
                bottom += axis;
            }
            if (v != 0) {
                barPaint.setColor(v < 0 ? (strong ? colorError : colorErrorLight)
                        : (strong ? colorPrimary : colorPrimaryLight));
                rect.set(cx - barW / 2f, top, cx + barW / 2f, bottom);
                path.reset();
                path.addRoundRect(rect, v < 0 ? radiiBottom : radiiTop, Path.Direction.CW);
                canvas.drawPath(path, barPaint);
            }
            if (i == highlight) {
                float boxTop = v < 0 ? bottom + TEXT_GAP_DP * density
                        : top - (TEXT_GAP_DP + TEXT_LINE_DP) * density;
                drawCentered(canvas, bars.get(i).valueText, cx, boxTop, valuePaint);
            }
            drawCentered(canvas, bars.get(i).label, cx, areaH + TEXT_GAP_DP * density, monthPaint);
        }
    }

    /** 在高 16dp 的行内垂直居中绘制文字 */
    private void drawCentered(Canvas canvas, String text, float cx, float boxTop, Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        float lineH = TEXT_LINE_DP * density;
        float y = boxTop + (lineH - (fm.descent - fm.ascent)) / 2f - fm.ascent;
        canvas.drawText(text, cx, y, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || bars.isEmpty()) return super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                int index = indexAt(event.getX());
                if (index >= 0) select(index);
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** x 坐标所在的列（列间距一半归入两侧的列） */
    int indexAt(float x) {
        int n = bars.size();
        if (n == 0 || getWidth() <= 0) return -1;
        float padH = PADDING_H_DP * density;
        float gap = GAP_DP * density;
        float colW = (getWidth() - 2 * padH - gap * (n - 1)) / n;
        int index = (int) Math.floor((x - padH + gap / 2f) / (colW + gap));
        return Math.max(0, Math.min(n - 1, index));
    }
}
