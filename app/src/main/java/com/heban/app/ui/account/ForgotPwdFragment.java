package com.heban.app.ui.account;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** A07 忘记密码 · 验证手机（占位页，M1 阶段实现） */
public class ForgotPwdFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "A07 忘记密码 · 验证手机";
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
                link("返回（A03）", R.id.loginFragment),
                link("手机号已停用、收不到验证码？ 申诉换绑（A09）", R.id.phoneAppealFragment),
                link("下一步（A08）", R.id.resetPwdFragment)
        };
    }
}
