package com.heban.app.ui.points;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** P04 兑换商品详情（占位页，M7 阶段实现） */
public class MallItemFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "P04 兑换商品详情";
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
                link("客服（P09）", R.id.serviceChatFragment),
                link("立即兑换 · 积分（P05）", R.id.mallConfirmFragment)
        };
    }
}
