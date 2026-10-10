package com.heban.app.ui.crop;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** C01 我的作物（占位页，M2 阶段实现） */
public class CropsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "C01 我的作物";
    }

    @Override
    protected String phase() {
        return "M2";
    }

    @Override
    protected boolean isTopLevel() {
        return true;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("地块管理（C05）", R.id.plotsFragment),
                link("添加作物（C03）", R.id.addCropFragment),
                link("番茄 · 樱桃番茄 结果期 1号大棚…（C02）", R.id.cropDetailFragment),
                link("黄瓜 霜霉病风险 2号大棚 · 0.（C02）", R.id.cropDetailFragment),
                link("冬小麦 分蘖期 东地块 · 3.0 …（C02）", R.id.cropDetailFragment),
                link("草莓 · 红颜 定植期 2号大棚 ·（C02）", R.id.cropDetailFragment),
                link("问 AI（D06）", R.id.aiChatFragment)
        };
    }
}
