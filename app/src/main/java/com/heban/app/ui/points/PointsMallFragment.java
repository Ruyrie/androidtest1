package com.heban.app.ui.points;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** P03 积分商城（占位页，M7 阶段实现） */
public class PointsMallFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "P03 积分商城";
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
                link("返回（H05）", R.id.checkInFragment),
                link("禾伴客服（P09）", R.id.serviceChatFragment),
                link("积分明细（P01）", R.id.pointsDetailFragment),
                link("兑换记录（P07）", R.id.ordersFragment),
                link("赚积分（H05）", R.id.checkInFragment),
                link("规则（P02）", R.id.pointsRulesFragment),
                link("实物 番茄种子组合（樱桃番茄 + 大…（P04）", R.id.mall_graph),
                link("实物 加厚园艺手套 双 积分（P04）", R.id.mall_graph),
                link("实物 不锈钢修枝剪 1,200 积分（P04）", R.id.mall_graph),
                link("实物 腐熟有机肥 1,500 积（P04）", R.id.mall_graph),
                link("装扮 · 即时到账 头像框「丰（P04）", R.id.mall_graph),
                link("装扮 · 即时到账 主页封面「麦浪」（P04）", R.id.mall_graph)
        };
    }
}
