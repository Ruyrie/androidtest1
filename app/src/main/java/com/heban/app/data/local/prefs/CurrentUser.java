package com.heban.app.data.local.prefs;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** 当前账号：本机数据按 user_id 隔离；游客和未登录时 userId 为 0（游客只能浏览）。 */
public final class CurrentUser {
    private final SettingsStore settings;

    public CurrentUser(SettingsStore settings) {
        this.settings = settings;
    }

    /** 当前登录账号 id，未登录 / 游客为 0；登录态变化时重新发射 */
    public Flowable<Long> observeUserId() {
        return settings.observe(SettingsStore.SESSION_USER_ID, 0L);
    }

    /** 取一次当前账号 id（未登录为 0） */
    public Single<Long> userId() {
        return settings.get(SettingsStore.SESSION_USER_ID, 0L);
    }

    /** 主线程可直接调用：是否已用账号登录（游客返回 false） */
    public boolean isSignedIn() {
        return settings.peek(SettingsStore.SESSION_USER_ID, 0L) > 0;
    }
}
