package com.heban.app.ui.home;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** H02 天气详情（占位页，M3 阶段实现） */
public class WeatherFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "H02 天气详情";
    }

    @Override
    protected String phase() {
        return "M3";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("返回（H01）", R.id.homeFragment)
        };
    }
}
