package com.heban.app.ui.discover.pest;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

/** D05 图片横向列表按张吸附：图片左边缘对齐列表内边距（第一张与页面边距对齐，后一张露出一部分）。 */
final class PestDetailSnapHelper extends LinearSnapHelper {
    @Nullable
    private OrientationHelper helper;

    private OrientationHelper helper(RecyclerView.LayoutManager lm) {
        if (helper == null || helper.getLayoutManager() != lm) {
            helper = OrientationHelper.createHorizontalHelper(lm);
        }
        return helper;
    }

    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager lm, @NonNull View target) {
        int[] out = new int[2];
        if (lm.canScrollHorizontally()) {
            OrientationHelper h = helper(lm);
            out[0] = h.getDecoratedStart(target) - h.getStartAfterPadding();
        }
        return out;
    }

    @Nullable
    @Override
    public View findSnapView(RecyclerView.LayoutManager lm) {
        if (!(lm instanceof LinearLayoutManager)) return super.findSnapView(lm);
        LinearLayoutManager llm = (LinearLayoutManager) lm;
        int first = llm.findFirstVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION) return null;
        // 已经滚到最后一张完整可见：不再吸附，避免来回弹动
        if (llm.findLastCompletelyVisibleItemPosition() == lm.getItemCount() - 1) return null;
        View v = lm.findViewByPosition(first);
        if (v == null) return null;
        OrientationHelper h = helper(lm);
        int visible = h.getDecoratedEnd(v) - h.getStartAfterPadding();
        if (visible >= h.getDecoratedMeasurement(v) / 2) return v;
        return lm.findViewByPosition(first + 1);
    }

    /** 当前页：滚到末尾时为最后一张，否则为第一张完整可见的 */
    static int currentPage(@NonNull LinearLayoutManager llm, int count) {
        if (count == 0) return 0;
        if (llm.findLastCompletelyVisibleItemPosition() == count - 1) return count - 1;
        int p = llm.findFirstCompletelyVisibleItemPosition();
        if (p == RecyclerView.NO_POSITION) p = llm.findFirstVisibleItemPosition();
        return Math.max(0, p);
    }
}
