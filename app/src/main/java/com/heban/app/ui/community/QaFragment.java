package com.heban.app.ui.community;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** S04 问答（占位页，M6 阶段实现） */
public class QaFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "S04 问答";
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
                link("消息，3 条未读（S08）", R.id.messagesFragment),
                link("推荐（S01）", R.id.communityFragment),
                link("番茄叶片卷曲发黄，是缺什么肥？ 中下…（S05）", R.id.questionFragment),
                link("黄瓜花多果少，怎么提高坐果率？ 王农…（S05）", R.id.questionFragment),
                link("草莓苗定植后叶片发红正常吗？ 定植一…（S05）", R.id.questionFragment),
                link("冬小麦播种量一亩多少合适？ AI 参…（S05）", R.id.questionFragment),
                link("提问（S03）", R.id.publishFragment),
                link("首页（H01）", R.id.homeFragment),
                link("作物（C01）", R.id.cropsFragment),
                link("发现（D01）", R.id.discoverFragment),
                link("农友圈（S01）", R.id.communityFragment),
                link("我的（M01）", R.id.profileFragment)
        };
    }
}
