package com.heban.app.ui.community;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** S06 农友主页（占位页，M6 阶段实现） */
public class UserProfileFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "S06 农友主页";
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
                link("私信（S09）", R.id.chatFragment),
                link("关注（S07）", R.id.followsFragment),
                link("1,203 粉丝（S07）", R.id.followsFragment),
                link("动态详情（S02）", R.id.postDetailFragment)
        };
    }
}
