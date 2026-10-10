package com.heban.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.heban.app.HeBanApp;
import com.heban.app.R;
import com.heban.app.databinding.ActivityMainBinding;
import com.heban.app.ui.common.BackPressExitHelper;
import com.heban.app.ui.common.NavRoutes;

import java.util.Set;

/**
 * 单 Activity：导航宿主、底部导航、「问 AI」按钮（技术准备文档 6.1）。
 * A01 闪屏使用 Android 12 原生 SplashScreen：启动设置在 Application 中同步读取，读取完成前保持闪屏。
 */
public class MainActivity extends AppCompatActivity {
    /** 通知点击后要打开的页面（导航目的地 id），仅在已登录（含游客）时生效 */
    public static final String EXTRA_OPEN_DEST = "open_dest";
    private static final Set<Integer> SHOW_AI_FAB = Set.of(
            R.id.homeFragment, R.id.cropsFragment, R.id.discoverFragment);
    private static final long EXIT_WINDOW_MS = 2000;

    private ActivityMainBinding binding;
    private NavController nav;
    private final BackPressExitHelper exitHelper = new BackPressExitHelper(EXIT_WINDOW_MS);
    private Insets systemBars = Insets.NONE;
    private Insets ime = Insets.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        nav = host.getNavController();
        nav.setGraph(NavRoutes.buildGraph(nav, HeBanApp.container(this).settings));

        NavigationUI.setupWithNavController(binding.bottomNav, nav);
        nav.addOnDestinationChangedListener((controller, dest, args) -> {
            binding.setShowBottomNav(NavRoutes.isTopLevel(dest.getId()));
            binding.setShowAiFab(SHOW_AI_FAB.contains(dest.getId()));
            binding.executePendingBindings();
            applyInsets();
        });
        binding.fabAi.setOnClickListener(v -> nav.navigate(R.id.aiChatFragment));
        if (savedInstanceState == null) openFromIntent(getIntent());

        ViewCompat.setOnApplyWindowInsetsListener(binding.root, (v, insets) -> {
            systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            applyInsets();
            return insets;
        });

        // 再按一次返回键退出：只在返回会离开 App 时生效（导航返回栈已到底）
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (nav.getPreviousBackStackEntry() != null) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                    return;
                }
                if (exitHelper.onBackPressed(SystemClock.uptimeMillis())) {
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, R.string.press_again_to_exit, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        openFromIntent(intent);
    }

    private void openFromIntent(Intent intent) {
        int dest = intent == null ? 0 : intent.getIntExtra(EXTRA_OPEN_DEST, 0);
        if (dest == 0 || !HeBanApp.container(this).settings.isLoggedIn() || NavRoutes.inAuthFlow(nav)) return;
        intent.removeExtra(EXTRA_OPEN_DEST);
        nav.navigate(dest);
    }

    /** 状态栏留给页面顶栏；底部导航隐藏时，页面自己避开导航栏和输入法 */
    private void applyInsets() {
        boolean bottomNavShown = binding.getShowBottomNav();
        int bottom = Math.max(ime.bottom, bottomNavShown ? 0 : systemBars.bottom);
        binding.navHost.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom);
    }
}
