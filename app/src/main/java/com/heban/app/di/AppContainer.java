package com.heban.app.di;

import android.content.Context;

import com.google.gson.Gson;
import com.heban.app.BuildConfig;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.remote.NetworkModule;
import com.heban.app.data.remote.sms.SmsService;
import com.heban.app.data.remote.sms.SmsServices;
import com.heban.app.data.repository.UserRepository;

import okhttp3.OkHttpClient;

/**
 * 手写依赖容器（技术准备文档 5.2）：在 Application 中集中创建单例对象。
 * 各阶段新增的数据库表、网络服务和 Repository 在这里装配。
 */
public final class AppContainer {
    public final Context app;
    public final Gson gson = new Gson();
    public final AppDatabase db;
    public final SettingsStore settings;
    public final SecureStore secureStore = new SecureStore();
    public final OkHttpClient baseClient;
    /** 当前 flavor 的数据来源："mock" 或 "remote" */
    public final String dataSource = RepositoryModule.DATA_SOURCE;

    public final SmsService smsService;
    public final UserRepository userRepository;     // 随 flavor 变化

    public AppContainer(Context context) {
        app = context.getApplicationContext();
        db = AppDatabase.get(app);
        settings = new SettingsStore(app);
        baseClient = NetworkModule.baseClient(BuildConfig.DEBUG);

        smsService = SmsServices.create(baseClient, gson);   // aliyun 或 fake

        userRepository = RepositoryModule.userRepository(this);
    }
}
