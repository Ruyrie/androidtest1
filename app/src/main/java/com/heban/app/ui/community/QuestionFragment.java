package com.heban.app.ui.community;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** S05 问题详情（占位页，M6 阶段实现） */
public class QuestionFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "S05 问题详情";
    }

    @Override
    protected String phase() {
        return "M6";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("返回（S04）", R.id.qaFragment),
                link("王（S06）", R.id.userProfileFragment),
                link("莓（S06）", R.id.userProfileFragment),
                link("周（S06）", R.id.userProfileFragment),
                link("补充描述（S03）", R.id.publishFragment)
        };
    }
}
