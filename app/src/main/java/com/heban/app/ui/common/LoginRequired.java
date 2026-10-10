package com.heban.app.ui.common;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.heban.app.HeBanApp;
import com.heban.app.R;

/**
 * 游客只能浏览：添加作物、记账、使用 AI、点赞评论等写操作前调用。
 * 未登录时弹出对话框引导登录并返回 false。
 */
public final class LoginRequired {
    private LoginRequired() {
    }

    public static boolean check(Fragment fragment) {
        if (HeBanApp.container(fragment.requireContext()).currentUser.isSignedIn()) return true;
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.login_required_title)
                .setMessage(R.string.login_required_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.login_required_go, (d, w) ->
                        NavRoutes.toLogin(NavHostFragment.findNavController(fragment)))
                .show();
        return false;
    }
}
