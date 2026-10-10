package com.heban.app.ui.account;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** A02 引导页（占位页，M1 阶段实现） */
public class OnboardingFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "A02 引导页";
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
                link("跳过（A03）", R.id.loginFragment),
                link("已有账号？ 登录（A03）", R.id.loginFragment)
        };
    }
}
