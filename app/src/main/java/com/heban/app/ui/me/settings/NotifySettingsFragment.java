package com.heban.app.ui.me.settings;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M09 消息通知（占位页，M7 阶段实现） */
public class NotifySettingsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M09 消息通知";
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
                link("返回（M05）", R.id.settingsFragment)
        };
    }
}
