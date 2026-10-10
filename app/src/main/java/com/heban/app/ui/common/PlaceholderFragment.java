package com.heban.app.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.heban.app.R;
import com.heban.app.databinding.FragmentPlaceholderBinding;

/**
 * M0 阶段的占位页：显示页面编号和名称，并按原型图的跳转关系列出跳转按钮。
 * 各页面在后续阶段替换为正式实现后，本类即可删除。
 */
public abstract class PlaceholderFragment extends BaseFragment {
    /** 从账号流程以外回到登录页 */
    public static final int TO_LOGIN = -1;

    protected static final class Link {
        final String label;
        final int dest;

        Link(String label, int dest) {
            this.label = label;
            this.dest = dest;
        }
    }

    protected static Link link(String label, int dest) {
        return new Link(label, dest);
    }

    protected abstract String pageTitle();

    protected abstract String phase();

    protected abstract boolean isTopLevel();

    protected abstract Link[] links();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentPlaceholderBinding b = FragmentPlaceholderBinding.inflate(inflater, container, false);
        b.toolbar.setTitle(pageTitle());
        if (!isTopLevel()) {
            b.toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            b.toolbar.setNavigationContentDescription(R.string.back);
            b.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }
        b.setPhase(getString(R.string.placeholder_phase, phase()));
        for (Link l : links()) {
            MaterialButton btn = (MaterialButton) inflater.inflate(R.layout.item_placeholder_link, b.links, false);
            btn.setText(l.label);
            btn.setOnClickListener(v -> go(l.dest));
            b.links.addView(btn);
        }
        return b.getRoot();
    }

    private void go(@IdRes int dest) {
        if (dest == TO_LOGIN) {
            NavRoutes.toLogin(nav());
        } else if (dest == R.id.homeFragment && NavRoutes.inAuthFlow(nav())) {
            NavRoutes.enterHome(nav());
        } else {
            nav().navigate(dest);
        }
    }
}
