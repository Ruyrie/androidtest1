package com.heban.app.ui.community;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** S08 消息（占位页，M6 阶段实现） */
public class MessagesFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "S08 消息";
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
                link("通知设置（M09）", R.id.notifySettingsFragment),
                link("评论回复（S02）", R.id.postDetailFragment),
                link("赞和收藏（S02）", R.id.postDetailFragment),
                link("新增关注（S07）", R.id.followsFragment),
                link("系统通知（S05）", R.id.questionFragment),
                link("莓 草莓姐姐 14:32 好的，明天…（S09）", R.id.chatFragment),
                link("禾 禾伴客服 08:45 你兑换的「…（P09）", R.id.serviceChatFragment),
                link("王 王农艺师 昨天 照片收到了，像是…（S09）", R.id.chatFragment),
                link("强 果园阿强 周四 [商品] 小型微…（S09）", R.id.chatFragment),
                link("禾 禾伴小助手 周三 你的提问「番茄…（S09）", R.id.chatFragment),
                link("周 菜园小周 10/05 谢谢老李，…（S09）", R.id.chatFragment)
        };
    }
}
