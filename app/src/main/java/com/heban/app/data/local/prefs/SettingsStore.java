package com.heban.app.data.local.prefs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
public final class SettingsStore implements SessionStore {
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
    // 天气地点（H02 切换地点后保存；为空时按定位或资料中的地区解析）
    public static final Preferences.Key<String> WEATHER_LOCATION_ID = PreferencesKeys.stringKey("weather.location_id");
    public static final Preferences.Key<String> WEATHER_LOCATION_NAME =
            PreferencesKeys.stringKey("weather.location_name");
    // 消息通知（M09）；免打扰用「当天分钟数」存储，默认 22:00 – 次日 06:30
    public static final Preferences.Key<Boolean> NOTIFY_ENABLED = PreferencesKeys.booleanKey("notify.enabled");
    public static final Preferences.Key<Boolean> NOTIFY_FARM_TASK = PreferencesKeys.booleanKey("notify.farm_task");
    public static final Preferences.Key<Boolean> NOTIFY_WEATHER = PreferencesKeys.booleanKey("notify.weather");
    public static final Preferences.Key<Boolean> NOTIFY_PEST = PreferencesKeys.booleanKey("notify.pest");
    public static final Preferences.Key<Boolean> NOTIFY_COMMENT = PreferencesKeys.booleanKey("notify.comment");
    public static final Preferences.Key<Boolean> NOTIFY_LIKE = PreferencesKeys.booleanKey("notify.like");
    public static final Preferences.Key<Boolean> NOTIFY_FOLLOW = PreferencesKeys.booleanKey("notify.follow");
    public static final Preferences.Key<Boolean> NOTIFY_DM = PreferencesKeys.booleanKey("notify.dm");
    public static final Preferences.Key<Integer> NOTIFY_DND_START = PreferencesKeys.intKey("notify.dnd_start");
    public static final Preferences.Key<Integer> NOTIFY_DND_END = PreferencesKeys.intKey("notify.dnd_end");
    public static final int DEFAULT_DND_START = 22 * 60;
    public static final int DEFAULT_DND_END = 6 * 60 + 30;

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

    // —— SessionStore：登录态 ——

    @Override
    public Flowable<Long> observeSessionUserId() {
        return observe(SESSION_USER_ID, 0L);
    }

    @Override
    public Single<String> sessionToken() {
        return get(SESSION_TOKEN, "");
    }

    @Override
    public Completable saveSession(long userId, @Nullable String encryptedToken) {
        return store.updateDataAsync(p -> {
            MutablePreferences m = p.toMutablePreferences();
            m.set(SESSION_USER_ID, userId);
            if (encryptedToken != null) {
                m.set(SESSION_TOKEN, encryptedToken);
            } else {
                m.remove(SESSION_TOKEN);
            }
            m.set(SESSION_GUEST, false);
            m.set(ONBOARDING_DONE, true);
            return Single.just(m);
        }).ignoreElement();
    }

    @Override
    public Completable clearSession() {
        return store.updateDataAsync(p -> {
            MutablePreferences m = p.toMutablePreferences();
            m.remove(SESSION_USER_ID);
            m.remove(SESSION_TOKEN);
            m.remove(SESSION_GUEST);
            return Single.just(m);
        }).ignoreElement();
    }

    @Override
    public Completable saveGuest() {
        return store.updateDataAsync(p -> {
            MutablePreferences m = p.toMutablePreferences();
            m.set(SESSION_GUEST, true);
            m.set(ONBOARDING_DONE, true);
            return Single.just(m);
        }).ignoreElement();
    }

    @Override
    public Completable saveOnboardingDone() {
        return put(ONBOARDING_DONE, true);
    }
}
