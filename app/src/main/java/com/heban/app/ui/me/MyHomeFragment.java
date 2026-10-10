package com.heban.app.ui.me;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M02 我的主页（占位页，M7 阶段实现） */
public class MyHomeFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M02 我的主页";
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
                link("返回（M01）", R.id.profileFragment),
                link("更换封面（M03）", R.id.editProfileFragment),
                link("编辑资料（M03）", R.id.editProfileFragment),
                link("关注（S07）", R.id.followsFragment),
                link("粉丝（S07）", R.id.followsFragment),
                link("徽章（H05）", R.id.checkInFragment),
                link("发动态（S03）", R.id.publishFragment),
                link("动态详情（S02）", R.id.postDetailFragment)
        };
    }
}
