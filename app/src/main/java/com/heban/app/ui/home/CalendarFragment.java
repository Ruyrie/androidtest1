package com.heban.app.ui.home;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** H03 农事日历（占位页，M2 阶段实现） */
public class CalendarFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "H03 农事日历";
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
                link("返回（H01）", R.id.homeFragment),
                link("新建农事（H04）", R.id.addTaskFragment)
        };
    }
}
