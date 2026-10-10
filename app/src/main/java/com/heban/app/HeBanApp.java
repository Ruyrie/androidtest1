package com.heban.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.heban.app.di.AppContainer;
import com.heban.app.di.AppViewModelFactory;

import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public class HeBanApp extends Application {
    private static final String TAG = "HeBan";

    private AppContainer container;
    private AppViewModelFactory factory;

    public static AppContainer container(Context context) {
        return ((HeBanApp) context.getApplicationContext()).container;
    }

    public static AppViewModelFactory factory(Context context) {
        return ((HeBanApp) context.getApplicationContext()).factory;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        factory = new AppViewModelFactory(container);

        // 启动时同步读取一次设置（主题、动态取色、登录态），系统闪屏在此期间保持显示
        container.settings.loadBlocking();

        // 显示与通用（M12）：浅色 / 深色 / 跟随系统
        AppCompatDelegate.setDefaultNightMode(container.settings.themeMode());

        // 动态取色开关：开启时按壁纸生成颜色角色（Android 12+）
        DynamicColors.applyToActivitiesIfAvailable(this, new DynamicColorsOptions.Builder()
                .setPrecondition((activity, theme) -> container.settings.dynamicColorEnabled())
                .build());

        RxJavaPlugins.setErrorHandler(e -> Log.w(TAG, "Undeliverable RxJava error", e));
        container.notifier.createChannels();
    }
}
