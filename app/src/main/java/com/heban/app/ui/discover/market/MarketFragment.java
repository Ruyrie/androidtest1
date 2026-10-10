package com.heban.app.ui.discover.market;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D07 农产品集市（占位页，M5 阶段实现） */
public class MarketFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D07 农产品集市";
    }

    @Override
    protected String phase() {
        return "M5";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("返回（D01）", R.id.discoverFragment),
                link("我的收藏（M04）", R.id.favoritesFragment),
                link("置顶 · 剩 小时 自家大棚樱桃番茄…（D08）", R.id.productFragment),
                link("产地直供 句容红颜草莓 当天现摘 斤…（D08）", R.id.productFragment),
                link("二手 小型微耕机 九成新 带旋耕刀 …（D08）", R.id.productFragment),
                link("可配送 腐熟羊粪有机肥 袋装 ¥38（D08）", R.id.productFragment),
                link("种苗 红颜草莓脱毒苗 现货 ¥0.6…（D08）", R.id.productFragment),
                link("发布（D09）", R.id.addProductFragment)
        };
    }
}
