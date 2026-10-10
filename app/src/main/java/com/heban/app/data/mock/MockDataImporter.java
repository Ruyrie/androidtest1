package com.heban.app.data.mock;

import android.content.Context;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.prefs.SettingsStore;

import io.reactivex.rxjava3.core.Completable;

/**
 * 模拟数据导入（技术准备文档 7.5）：第一次启动或 MOCK_VERSION 提高时，在后台把 assets/mock/ 中
 * 「将来由后端提供」的共享内容（病虫害百科、集市商品，以及后续的农友、动态、问答、积分商品）写入 Room。
 * 用户自己的地块、作物、农事、记账、积分不导入（新账号从空开始）。
 */
public final class MockDataImporter {
    private final Context app;
    private final AppDatabase db;
    private final SettingsStore settings;
    private final Gson gson;

    public MockDataImporter(Context app, AppDatabase db, SettingsStore settings, Gson gson) {
        this.app = app;
        this.db = db;
        this.settings = settings;
        this.gson = gson;
    }

    public Completable importIfNeeded() {
        return Completable.complete();
    }
}
