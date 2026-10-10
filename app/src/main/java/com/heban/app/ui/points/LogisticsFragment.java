package com.heban.app.ui.points;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** P08 物流详情（占位页，M7 阶段实现） */
public class LogisticsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "P08 物流详情";
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
                link("返回（P07）", R.id.ordersFragment),
                link("联系禾伴客服（P09）", R.id.serviceChatFragment)
        };
    }
}
