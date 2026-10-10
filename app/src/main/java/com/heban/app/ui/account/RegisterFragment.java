package com.heban.app.ui.account;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** A05 注册（占位页，M1 阶段实现） */
public class RegisterFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "A05 注册";
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
                link("《用户协议》（M13）", R.id.aboutFragment),
                link("《隐私政策》（M13）", R.id.aboutFragment),
                link("下一步（A06）", R.id.profileSetupFragment),
                link("已有账号？ 去登录（A03）", R.id.loginFragment)
        };
    }
}
