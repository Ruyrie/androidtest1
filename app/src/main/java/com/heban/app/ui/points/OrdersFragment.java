package com.heban.app.ui.points;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** P07 兑换记录（占位页，M7 阶段实现） */
public class OrdersFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "P07 兑换记录";
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
                link("返回（P03）", R.id.pointsMallFragment),
                link("禾伴客服（P09）", R.id.serviceChatFragment),
                link("联系客服（P09）", R.id.serviceChatFragment),
                link("修改地址（P06）", R.id.addressFragment),
                link("查看物流（P08）", R.id.logisticsFragment),
                link("去使用（M03）", R.id.editProfileFragment),
                link("再次兑换（P03）", R.id.pointsMallFragment)
        };
    }
}
