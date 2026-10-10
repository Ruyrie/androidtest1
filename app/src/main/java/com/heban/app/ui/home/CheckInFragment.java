package com.heban.app.ui.home;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** H05 签到与成就（占位页，M7 阶段实现） */
public class CheckInFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "H05 签到与成就";
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
                link("返回（H01）", R.id.homeFragment),
                link("积分规则（P02）", R.id.pointsRulesFragment),
                link("查看积分明细（P01）", R.id.pointsDetailFragment),
                link("积分商城 种子、农具、头像框，实物官…（P03）", R.id.pointsMallFragment),
                link("去完成（S03）", R.id.publishFragment),
                link("去回答（S04）", R.id.qaFragment),
                link("去记账（D11）", R.id.addEntryFragment)
        };
    }
}
