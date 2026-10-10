package com.heban.app.ui.me;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M01 我的（占位页，M7 阶段实现） */
public class ProfileFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M01 我的";
    }

    @Override
    protected String phase() {
        return "M7";
    }

    @Override
    protected boolean isTopLevel() {
        return true;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("我的名片（M02）", R.id.myHomeFragment),
                link("设置（M05）", R.id.settingsFragment),
                link("李 老李种菜 Lv.4 种植能手 江…（M02）", R.id.myHomeFragment),
                link("动态（M02）", R.id.myHomeFragment),
                link("关注（S07）", R.id.followsFragment),
                link("粉丝（S07）", R.id.followsFragment),
                link("1.6k 获赞（M02）", R.id.myHomeFragment),
                link("已连续签到（H05）", R.id.checkInFragment),
                link("我的动态（M02）", R.id.myHomeFragment),
                link("我的问答（S04）", R.id.qaFragment),
                link("我的收藏（M04）", R.id.favoritesFragment),
                link("我的发布（D07）", R.id.marketFragment),
                link("收支记账（D10）", R.id.ledgerFragment),
                link("地块管理（C05）", R.id.plotsFragment),
                link("成就徽章（H05）", R.id.checkInFragment),
                link("积分商城（P03）", R.id.pointsMallFragment),
                link("关于禾伴 v1.0.0（M13）", R.id.aboutFragment)
        };
    }
}
