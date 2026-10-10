package com.heban.app.ui.common;

import androidx.annotation.IdRes;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;

import com.heban.app.R;
import com.heban.app.data.local.prefs.SettingsStore;

/** 跨导航图的跳转（登录前后切换起始页、进入首页、回到登录页）。 */
public final class NavRoutes {
    private NavRoutes() {
    }

    /** 根据登录态生成主导航图：已登录（含游客）进入首页，否则进入账号流程（引导页或登录页）。 */
    public static NavGraph buildGraph(NavController nav, SettingsStore settings) {
        NavGraph graph = nav.getNavInflater().inflate(R.navigation.nav_graph);
        NavGraph auth = (NavGraph) graph.findNode(R.id.auth_graph);
        if (auth != null) {
            auth.setStartDestination(settings.onboardingDone() ? R.id.loginFragment : R.id.onboardingFragment);
        }
        graph.setStartDestination(settings.isLoggedIn() ? R.id.homeFragment : R.id.auth_graph);
        return graph;
    }

    /** 登录成功或游客浏览：进入首页并清空账号流程的返回栈 */
    public static void enterHome(NavController nav) {
        NavGraph graph = nav.getGraph();
        graph.setStartDestination(R.id.homeFragment);
        nav.navigate(R.id.homeFragment, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build());
    }

    /** 退出登录 / 切换账号：回到登录页并清空返回栈 */
    public static void toLogin(NavController nav) {
        NavGraph graph = nav.getGraph();
        NavGraph auth = (NavGraph) graph.findNode(R.id.auth_graph);
        if (auth != null) auth.setStartDestination(R.id.loginFragment);
        graph.setStartDestination(R.id.auth_graph);
        nav.navigate(R.id.auth_graph, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build());
    }

    /** 当前页面是否在账号流程（auth_graph）中 */
    public static boolean inAuthFlow(NavController nav) {
        NavDestination d = nav.getCurrentDestination();
        while (d != null) {
            if (d.getId() == R.id.auth_graph) return true;
            d = d.getParent();
        }
        return false;
    }

    public static boolean isTopLevel(@IdRes int destId) {
        return destId == R.id.homeFragment || destId == R.id.cropsFragment
                || destId == R.id.discoverFragment || destId == R.id.communityFragment
                || destId == R.id.profileFragment;
    }
}
