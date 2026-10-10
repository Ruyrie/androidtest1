package com.heban.app.ui.me.settings;

import com.heban.app.R;
import com.heban.app.ui.common.PlaceholderFragment;

/** M05 设置（占位页，M7 阶段实现） */
public class SettingsFragment extends PlaceholderFragment {

    @Override
    protected String pageTitle() {
        return "M05 设置";
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
                link("返回（M01）", R.id.profileFragment),
                link("账号与安全 手机号 138***（M06）", R.id.securityFragment),
                link("隐私设置 主页可见范围、私信权限、黑…（M10）", R.id.privacySettingsFragment),
                link("消息通知 农事提醒、互动消息、免（M09）", R.id.notifySettingsFragment),
                link("显示与通用 深色模式、字体大小、清理…（M12）", R.id.generalSettingsFragment),
                link("AI 助手设置 回答风格、结合我的（M11）", R.id.aiSettingsFragment),
                link("关于禾伴 版本 1.0.0（M13）", R.id.aboutFragment),
                link("切换账号（A03）", PlaceholderFragment.TO_LOGIN)
        };
    }
}
