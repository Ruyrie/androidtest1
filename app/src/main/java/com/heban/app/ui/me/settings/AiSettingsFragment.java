package com.heban.app.ui.me.settings;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M11 AI 助手设置（占位页，M4 阶段实现） */
public class AiSettingsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M11 AI 助手设置";
    }

    @Override
    protected String phase() {
        return "M4";
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
