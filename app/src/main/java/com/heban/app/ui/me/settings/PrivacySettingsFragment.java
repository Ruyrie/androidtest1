package com.heban.app.ui.me.settings;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M10 隐私设置（占位页，M7 阶段实现） */
public class PrivacySettingsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M10 隐私设置";
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
                link("返回（M05）", R.id.settingsFragment),
                link("隐私政策 2026年9月更新（M13）", R.id.aboutFragment)
        };
    }
}
