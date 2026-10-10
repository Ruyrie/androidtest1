package com.heban.app.ui.points;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** P06 收货地址（占位页，M7 阶段实现） */
public class AddressFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "P06 收货地址";
    }

    @Override
    protected String phase() {
        return "M7";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("返回（P05）", R.id.mall_graph)
        };
    }
}
