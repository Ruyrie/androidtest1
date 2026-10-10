package com.heban.app.ui.me.settings;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M06 账号与安全（占位页，M7 阶段实现） */
public class SecurityFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M06 账号与安全";
    }

    @Override
    protected String phase() {
        return "M7";
    }

    @Override
    protected boolean isTopLevel() {
        return false;
    }

    @Override
    protected Link[] links() {
        return new Link[]{
                link("返回（M05）", R.id.settingsFragment),
                link("手机号 138****5678 · 用（M08）", R.id.changePhoneFragment),
                link("修改密码 短信验证后设置新密码，无需…（M07）", R.id.changePwdFragment)
        };
    }
}
