package com.heban.app.ui.discover.market;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D08 商品详情（占位页，M5 阶段实现） */
public class ProductFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D08 商品详情";
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
                link("返回（D07）", R.id.marketFragment),
                link("莓 草莓姐姐 句容白兔镇 · 种植 年（S06）", R.id.userProfileFragment),
                link("私信（S09）", R.id.chatFragment),
                link("联系卖家（S09）", R.id.chatFragment)
        };
    }
}
