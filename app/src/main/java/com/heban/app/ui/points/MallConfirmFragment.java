package com.heban.app.ui.points;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** P05 确认兑换（占位页，M7 阶段实现） */
public class MallConfirmFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "P05 确认兑换";
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
                link("返回（P04）", R.id.mallItemFragment),
                link("老李 138****5678 默认（P06）", R.id.addressFragment)
        };
    }
}
