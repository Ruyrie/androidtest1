package com.heban.app.ui.account;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** A08 设置新密码（占位页，M1 阶段实现） */
public class ResetPwdFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "A08 设置新密码";
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
                link("返回（A07）", R.id.forgotPwdFragment)
        };
    }
}
