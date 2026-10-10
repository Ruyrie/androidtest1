package com.heban.app.ui.me.settings;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M12 显示与通用（占位页，M7 阶段实现） */
public class GeneralSettingsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M12 显示与通用";
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
