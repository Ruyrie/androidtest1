package com.heban.app.ui.account;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** A04 验证码登录（占位页，M1 阶段实现） */
public class LoginSmsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "A04 验证码登录";
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
                link("游客浏览（H01）", R.id.homeFragment),
                link("密码登录（A03）", R.id.loginFragment),
                link("《用户协议》（M13）", R.id.aboutFragment),
                link("《隐私政策》（M13）", R.id.aboutFragment),
                link("登录 / 注册（H01）", R.id.homeFragment)
        };
    }
}
