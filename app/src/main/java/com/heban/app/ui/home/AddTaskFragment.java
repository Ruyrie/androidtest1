package com.heban.app.ui.home;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** H04 新建农事（占位页，M2 阶段实现） */
public class AddTaskFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "H04 新建农事";
    }

    @Override
    protected String phase() {
        return "M2";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("关闭（H03）", R.id.calendarFragment),
                link("保存（H03）", R.id.calendarFragment)
        };
    }
}
