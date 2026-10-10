package com.heban.app.ui.account;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** A06 完善资料（占位页，M1 阶段实现） */
public class ProfileSetupFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "A06 完善资料";
    }

    @Override
    protected String phase() {
        return "M1";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("返回（A05）", R.id.registerFragment),
                link("跳过（H01）", R.id.homeFragment),
                link("进入禾伴（H01）", R.id.homeFragment)
        };
    }
}
