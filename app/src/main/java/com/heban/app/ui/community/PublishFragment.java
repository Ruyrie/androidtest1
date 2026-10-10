package com.heban.app.ui.community;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** S03 发布动态 / 提问（占位页，M6 阶段实现） */
public class PublishFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "S03 发布动态 / 提问";
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
                link("关闭（S01）", R.id.communityFragment),
                link("发布（S01）", R.id.communityFragment)
        };
    }
}
