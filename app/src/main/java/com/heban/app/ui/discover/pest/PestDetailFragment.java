package com.heban.app.ui.discover.pest;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** D05 病害详情（占位页，M5 阶段实现） */
public class PestDetailFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "D05 病害详情";
    }

    @Override
    protected String phase() {
        return "M5";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("返回（D04）", R.id.pestsFragment),
                link("问问 AI（D06）", R.id.aiChatFragment),
                link("加入农事日历（H04）", R.id.addTaskFragment)
        };
    }
}
