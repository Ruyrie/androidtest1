package com.heban.app.ui.community;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** S09 私信聊天（占位页，M6 阶段实现） */
public class ChatFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "S09 私信聊天";
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
                link("返回（S08）", R.id.messagesFragment),
                link("莓 草莓姐姐 种植能手 · 通常 小…（S06）", R.id.userProfileFragment),
                link("句容红颜草莓 当天现摘 ¥25 /斤…（D08）", R.id.productFragment)
        };
    }
}
