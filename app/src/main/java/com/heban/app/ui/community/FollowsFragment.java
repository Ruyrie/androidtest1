package com.heban.app.ui.community;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** S07 关注与粉丝（占位页，M6 阶段实现） */
public class FollowsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "S07 关注与粉丝";
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
                link("返回（M02）", R.id.myHomeFragment),
                link("周（S06）", R.id.userProfileFragment),
                link("菜园小周 江宁 · 家庭菜园（S06）", R.id.userProfileFragment),
                link("莓（S06）", R.id.userProfileFragment),
                link("草莓姐姐 种植能手 句容 · 种草莓…（S06）", R.id.userProfileFragment),
                link("陈（S06）", R.id.userProfileFragment),
                link("江宁老陈 种了 年小麦水稻（S06）", R.id.userProfileFragment),
                link("王（S06）", R.id.userProfileFragment),
                link("王农艺师 农技专家 区农技站 · 蔬…（S06）", R.id.userProfileFragment),
                link("强（S06）", R.id.userProfileFragment),
                link("果园阿强 溧水 · 梨和桃（S06）", R.id.userProfileFragment),
                link("林（S06）", R.id.userProfileFragment),
                link("小林的阳台菜园 新手 · 刚开始种番茄（S06）", R.id.userProfileFragment)
        };
    }
}
