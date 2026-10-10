package com.heban.app.ui.crop;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** C05 地块管理（占位页，M2 阶段实现） */
public class PlotsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "C05 地块管理";
    }

    @Override
    protected String phase() {
        return "M2";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("返回（C01）", R.id.cropsFragment),
                link("番茄 · 结果期（C02）", R.id.cropDetailFragment),
                link("黄瓜 · 0.4 亩（C02）", R.id.cropDetailFragment),
                link("草莓 · 0.3 亩（C02）", R.id.cropDetailFragment),
                link("冬小麦 · 分蘖期（C02）", R.id.cropDetailFragment)
        };
    }
}
