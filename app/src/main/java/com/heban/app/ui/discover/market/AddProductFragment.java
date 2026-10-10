package com.heban.app.ui.discover.market;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D09 发布商品（占位页，M5 阶段实现） */
public class AddProductFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D09 发布商品";
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
                link("关闭（D07）", R.id.marketFragment),
                link("发布（D08）", R.id.productFragment)
        };
    }
}
