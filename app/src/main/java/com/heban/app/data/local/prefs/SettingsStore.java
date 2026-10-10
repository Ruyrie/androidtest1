package com.heban.app.data.local.prefs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 设置项与登录态（DataStore Preferences，技术准备文档 7.3）。
 * 整个 App 只创建一个实例，放在 AppContainer 中。
 */
public final class SettingsStore {
    // 登录态
    public static final Preferences.Key<Long> SESSION_USER_ID = PreferencesKeys.longKey("session.user_id");
    public static final Preferences.Key<String> SESSION_TOKEN = PreferencesKeys.stringKey("session.token");
    public static final Preferences.Key<Boolean> SESSION_GUEST = PreferencesKeys.booleanKey("session.guest");
    public static final Preferences.Key<Boolean> ONBOARDING_DONE =
            PreferencesKeys.booleanKey("onboarding.done");
    // 显示
    public static final Preferences.Key<Integer> THEME_MODE = PreferencesKeys.intKey("ui.theme_mode");
    public static final Preferences.Key<Boolean> DYNAMIC_COLOR = PreferencesKeys.booleanKey("ui.dynamic_color");
    public static final Preferences.Key<Float> FONT_SCALE = PreferencesKeys.floatKey("ui.font_scale");

    private final RxDataStore<Preferences> store;
    private volatile Preferences snapshot;

    public SettingsStore(Context context) {
        store = new RxPreferenceDataStoreBuilder(context, "settings").build();
    }

    /** 只在 Application 启动时调用一次：同步读取，并在之后持续更新内存快照。 */
    public void loadBlocking() {
        snapshot = store.data().blockingFirst();
        //noinspection ResultOfMethodCallIgnored
        store.data().subscribe(p -> snapshot = p, e -> { });
    }

    public <T> Flowable<T> observe(Preferences.Key<T> key, @NonNull T defaultValue) {
        return store.data().map(p -> {
            T v = p.get(key);
            return v != null ? v : defaultValue;
        }).distinctUntilChanged();
    }

    public <T> Single<T> get(Preferences.Key<T> key, @NonNull T defaultValue) {
        return observe(key, defaultValue).firstOrError();
    }

    public <T> Completable put(Preferences.Key<T> key, T value) {
        return store.updateDataAsync(p -> {
            MutablePreferences m = p.toMutablePreferences();
            m.set(key, value);
            return Single.just(m);
        }).ignoreElement();
    }

    public <T> Completable remove(Preferences.Key<T> key) {
        return store.updateDataAsync(p -> {
            MutablePreferences m = p.toMutablePreferences();
            m.remove(key);
            return Single.just(m);
        }).ignoreElement();
    }

    /** 从启动时读取的快照中取值（主线程可直接调用） */
    public <T> T peek(Preferences.Key<T> key, @NonNull T defaultValue) {
        Preferences p = snapshot;
        T v = p == null ? null : p.get(key);
        return v != null ? v : defaultValue;
    }

    public int themeMode() {
        return peek(THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public boolean dynamicColorEnabled() {
        return peek(DYNAMIC_COLOR, false);
    }

    public boolean isLoggedIn() {
        return peek(SESSION_USER_ID, 0L) > 0 || peek(SESSION_GUEST, false);
    }

    public boolean onboardingDone() {
        return peek(ONBOARDING_DONE, false);
    }
}
