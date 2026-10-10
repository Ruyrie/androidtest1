package com.heban.app.di;

import android.content.Context;

import com.google.gson.Gson;
import com.heban.app.data.local.prefs.SettingsStore;

/**
 * 手写依赖容器（技术准备文档 5.2）：在 Application 中集中创建单例对象。
 * 数据库、网络、各 Repository 在对应开发阶段加入。
 */
public final class AppContainer {
    public final Context app;
    public final Gson gson = new Gson();
    public final SettingsStore settings;
    /** 当前 flavor 的数据来源："mock" 或 "remote" */
    public final String dataSource = RepositoryModule.DATA_SOURCE;

    public AppContainer(Context context) {
        app = context.getApplicationContext();
        settings = new SettingsStore(app);
    }
}
