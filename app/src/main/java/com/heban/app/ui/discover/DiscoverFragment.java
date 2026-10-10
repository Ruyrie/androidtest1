package com.heban.app.ui.discover;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D01 发现（占位页，M5 阶段实现） */
public class DiscoverFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D01 发现";
    }

    @Override
    protected String phase() {
        return "M5";
    }

    @Override
    protected boolean isTopLevel() {
        return true;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("拍照识别（D02）", R.id.scan_graph),
                link("拍照识别 拍一拍，认（D02）", R.id.scan_graph),
                link("病虫害百科 种 · 可离线查看（D04）", R.id.pestsFragment),
                link("农产品集市 附近 件在售（D07）", R.id.marketFragment),
                link("收支记账 本月（D10）", R.id.ledgerFragment),
                link("高发 霜霉病 黄瓜 · 白菜（D05）", R.id.pestDetailFragment),
                link("较高 灰霉病 番茄 · 草莓（D05）", R.id.pestDetailFragment),
                link("较高 蚜虫 多种蔬菜（D05）", R.id.pestDetailFragment),
                link("逛集市（D07）", R.id.marketFragment),
                link("句容红颜草莓 现摘 ¥25 /斤 草…（D08）", R.id.productFragment),
                link("小型微耕机 九成新 ¥1,800 果…（D08）", R.id.productFragment),
                link("问 AI（D06）", R.id.aiChatFragment)
        };
    }
}
