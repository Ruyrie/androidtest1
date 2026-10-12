package com.heban.app.ui.discover.ledger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * D10 支出构成（技术准备文档 12.4：自定义 View，Canvas 绘制；原型 Ledger「支出构成」）。
 * 每类一行：类别名（44dp 列，名字更长时加宽）· 横向条（高 12dp，右侧圆角 4dp，底色 Surface Container Highest，
 * 填充 Primary，长度相对最大一类）·「¥720 · 41%」（96dp 列，右对齐）。列间距 12dp，行间距 14dp，整体垂直居中。
 * 文字用 On Surface / On Surface Variant；没有支出时居中显示一行提示。
 */
public class LedgerMixChartView extends View {
    private static final float DEFAULT_HEIGHT_DP = 176f;
    private static final float NAME_COL_DP = 44f;
    private static final float VALUE_COL_DP = 96f;
    private static final float GAP_DP = 12f;
    private static final float ROW_GAP_DP = 14f;
    private static final float ROW_HEIGHT_DP = 20f;
    private static final float TRACK_HEIGHT_DP = 12f;
    private static final float CORNER_DP = 4f;

    private final float density;
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final float[] radiiEnd;

    private List<LedgerChartModel.MixRow> rows = Collections.emptyList();
    @Nullable private String emptyText;

    public LedgerMixChartView(@NonNull Context context) {
        this(context, null);
    }

    public LedgerMixChartView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        float r = CORNER_DP * density;
        radiiEnd = new float[]{0, 0, r, r, r, r, 0, 0};

        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics());
        namePaint.setTextSize(textSize);
        namePaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
        valuePaint.setTextSize(textSize);
        valuePaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        emptyPaint.setTextSize(textSize);
        emptyPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        trackPaint.setColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainerHighest));
        fillPaint.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
    }

    public void setRows(@Nullable List<LedgerChartModel.MixRow> list) {
        rows = list == null ? Collections.emptyList() : new ArrayList<>(list);
        setContentDescription(describe());
        invalidate();
    }

    /** 没有支出时显示的提示 */
    public void setEmptyText(@Nullable String text) {
        emptyText = text;
        if (rows.isEmpty()) setContentDescription(text);
        invalidate();
    }

    private String describe() {
        if (rows.isEmpty()) return emptyText;
        List<String> parts = new ArrayList<>();
        for (LedgerChartModel.MixRow r : rows) parts.add(r.name + " " + r.valueText);
        return String.join("，", parts);
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
        float width = getWidth();
        float height = getHeight();
        float rowH = ROW_HEIGHT_DP * density;
        if (rows.isEmpty()) {
            if (emptyText != null) drawInRow(canvas, emptyText, width / 2f, (height - rowH) / 2f, emptyPaint);
            return;
        }
        float gap = GAP_DP * density;
        float nameW = NAME_COL_DP * density;
        float valueW = VALUE_COL_DP * density;
        for (LedgerChartModel.MixRow r : rows) {
            nameW = Math.max(nameW, namePaint.measureText(r.name));
            valueW = Math.max(valueW, valuePaint.measureText(r.valueText));
        }
        float trackLeft = nameW + gap;
        float trackRight = width - valueW - gap;
        float trackW = Math.max(0, trackRight - trackLeft);
        float trackH = TRACK_HEIGHT_DP * density;

        int n = rows.size();
        float total = n * rowH + (n - 1) * ROW_GAP_DP * density;
        float top = (height - total) / 2f;
        for (LedgerChartModel.MixRow r : rows) {
            drawInRow(canvas, r.name, 0, top, namePaint);
            float barTop = top + (rowH - trackH) / 2f;
            rect.set(trackLeft, barTop, trackLeft + trackW, barTop + trackH);
            path.reset();
            path.addRoundRect(rect, radiiEnd, Path.Direction.CW);
            canvas.drawPath(path, trackPaint);
            float fillW = trackW * r.fraction;
            if (fillW > 0) {
                rect.set(trackLeft, barTop, trackLeft + fillW, barTop + trackH);
                path.reset();
                path.addRoundRect(rect, radiiEnd, Path.Direction.CW);
                canvas.drawPath(path, fillPaint);
            }
            drawInRow(canvas, r.valueText, width, top, valuePaint);
            top += rowH + ROW_GAP_DP * density;
        }
    }

    /** 在高 20dp 的行内垂直居中绘制文字（x 按画笔的对齐方式解释） */
    private void drawInRow(Canvas canvas, String text, float x, float rowTop, Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        float rowH = ROW_HEIGHT_DP * density;
        float y = rowTop + (rowH - (fm.descent - fm.ascent)) / 2f - fm.ascent;
        canvas.drawText(text, x, y, paint);
    }
}
