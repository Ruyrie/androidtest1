package com.heban.app.ui.discover;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 列表间距：横向列表只在卡片之间留 gap；网格（GridLayoutManager）列间、行间留 gap，
 * 跨满整行的条目（如 D04 列表头）不加间距。
 */
public final class DiscoverSpacing extends RecyclerView.ItemDecoration {
    private final int gap;

    public DiscoverSpacing(int gapPx) {
        this.gap = gapPx;
    }

    @Override
    public void getItemOffsets(@NonNull Rect out, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos == RecyclerView.NO_POSITION) return;
        RecyclerView.LayoutManager lm = parent.getLayoutManager();
        if (lm instanceof GridLayoutManager) {
            GridLayoutManager glm = (GridLayoutManager) lm;
            int spans = glm.getSpanCount();
            GridLayoutManager.SpanSizeLookup lookup = glm.getSpanSizeLookup();
            if (lookup.getSpanSize(pos) >= spans) return;
            int column = lookup.getSpanIndex(pos, spans);
            int row = lookup.getSpanGroupIndex(pos, spans);
            int firstRow = lookup.getSpanGroupIndex(firstGridPosition(lookup, spans, state.getItemCount()), spans);
            // 每列共分 gap：左列右侧 gap/2，右列左侧 gap/2（两列时宽度相同）
            out.left = column * gap / spans;
            out.right = gap - (column + 1) * gap / spans;
            out.top = row > firstRow ? gap : 0;
        } else {
            boolean rtl = parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            if (pos > 0) {
                if (rtl) out.right = gap;
                else out.left = gap;
            }
        }
    }

    private static int firstGridPosition(GridLayoutManager.SpanSizeLookup lookup, int spans, int count) {
        for (int i = 0; i < count; i++) {
            if (lookup.getSpanSize(i) < spans) return i;
        }
        return 0;
    }
}
