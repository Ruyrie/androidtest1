package com.heban.app.ui.discover.ai;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D06 AI 助手（占位页，M4 阶段实现） */
public class AiChatFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D06 AI 助手";
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
                link("返回（H01）", R.id.homeFragment),
                link("AI 助手设置（M11）", R.id.aiSettingsFragment),
                link("霜霉病百科（D05）", R.id.pestDetailFragment)
        };
    }
}
