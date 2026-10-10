package com.heban.app.ui.community;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** S02 动态详情（占位页，M6 阶段实现） */
public class PostDetailFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "S02 动态详情";
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
                link("返回（S01）", R.id.communityFragment),
                link("草莓姐姐的主页（S06）", R.id.userProfileFragment),
                link("草莓姐姐 种植能手 · 句容（S06）", R.id.userProfileFragment)
        };
    }
}
