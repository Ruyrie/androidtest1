package com.heban.app.ui.points;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** P09 禾伴客服（占位页，M7 阶段实现） */
public class ServiceChatFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "P09 禾伴客服";
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
                link("返回（S08）", R.id.messagesFragment),
                link("加厚园艺手套 双 运输中 · 预计明（P08）", R.id.logisticsFragment)
        };
    }
}
