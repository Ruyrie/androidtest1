package com.heban.app.ui.crop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;
import com.heban.app.R;
import com.heban.app.data.model.StageInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * C02 生长阶段步进条：每个阶段等分一列（24dp 圆点 + 名称），相邻圆点之间 2dp 连线；
 * 已完成 Primary 实心 + 勾，当前 Primary Container + 描边 + 圆点（名称加粗 Primary），未开始 Outline Variant 描边。
 */
public class CropDetailStepperView extends LinearLayout {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<StageInfo> stages = new ArrayList<>();
    private final float lineHeight;
    private final float circleCenterY;

    public CropDetailStepperView(Context context) {
        this(context, null);
    }

    public CropDetailStepperView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);
        float density = getResources().getDisplayMetrics().density;
        lineHeight = 2 * density;
        circleCenterY = 12 * density;
    }

    public void setStages(@Nullable List<StageInfo> list) {
        stages.clear();
        if (list != null) stages.addAll(list);
        removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (StageInfo s : stages) {
            View item = inflater.inflate(R.layout.crop_detail_item_stage, this, false);
            LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            item.setLayoutParams(lp);
            bindItem(item, s);
            addView(item);
        }
        invalidate();
    }

    private void bindItem(View item, StageInfo s) {
        View circle = item.findViewById(R.id.stage_circle);
        ImageView check = item.findViewById(R.id.stage_check);
        View dot = item.findViewById(R.id.stage_dot);
        TextView label = item.findViewById(R.id.stage_label);
        label.setText(CropsFormat.stepLabel(s.name));
        if (s.current) {
            circle.setBackgroundResource(R.drawable.crop_detail_stage_current);
            check.setVisibility(GONE);
            dot.setVisibility(VISIBLE);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setTextColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
        } else if (s.done) {
            circle.setBackgroundResource(R.drawable.crop_detail_stage_done);
            check.setVisibility(VISIBLE);
            dot.setVisibility(GONE);
        } else {
            circle.setBackgroundResource(R.drawable.crop_detail_stage_todo);
            check.setVisibility(GONE);
            dot.setVisibility(GONE);
        }
        item.setContentDescription(label.getText());
    }

    /** 连线画在圆点下层：第 i 段颜色取决于第 i 个阶段是否已完成 */
    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        int n = Math.min(stages.size(), getChildCount());
        if (n > 1) {
            int done = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary);
            int todo = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant);
            float top = getPaddingTop() + circleCenterY - lineHeight / 2f;
            for (int i = 0; i < n - 1; i++) {
                View a = getChildAt(i);
                View b = getChildAt(i + 1);
                float x1 = (a.getLeft() + a.getRight()) / 2f;
                float x2 = (b.getLeft() + b.getRight()) / 2f;
                linePaint.setColor(stages.get(i).done && !stages.get(i).current ? done : todo);
                canvas.drawRect(x1, top, x2, top + lineHeight, linePaint);
            }
        }
        super.dispatchDraw(canvas);
    }
}
