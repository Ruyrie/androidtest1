package com.heban.app.ui.points;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** P01 积分明细（占位页，M7 阶段实现） */
public class PointsDetailFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "P01 积分明细";
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
                link("积分规则（P02）", R.id.pointsRulesFragment),
                link("去兑换（P03）", R.id.pointsMallFragment)
        };
    }
}
