package com.heban.app.ui.me.settings;

import androidx.annotation.NonNull;

import com.heban.app.data.local.prefs.SettingsStore;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/** M11 中保存在 DataStore 的 AI 设置（回答风格与参考内容开关），抽成接口便于单元测试。 */
public interface AiSettingsPrefs {
    String STYLE_BRIEF = "BRIEF";
    String STYLE_DETAIL = "DETAIL";
    String STYLE_PLAIN = "PLAIN";

    enum Flag { USE_CROPS, USE_WEATHER, SAVE_HISTORY }

    Flowable<String> observeStyle();

    Completable setStyle(@NonNull String style);

    Flowable<Boolean> observeFlag(@NonNull Flag flag);

    Completable setFlag(@NonNull Flag flag, boolean value);

    /** SettingsStore 实现：默认「详细」，三个开关默认开启 */
    static AiSettingsPrefs of(@NonNull SettingsStore store) {
        return new AiSettingsPrefs() {
            @Override
            public Flowable<String> observeStyle() {
                return store.observe(SettingsStore.AI_STYLE, STYLE_DETAIL);
            }

            @Override
            public Completable setStyle(@NonNull String style) {
                return store.put(SettingsStore.AI_STYLE, style);
            }

            @Override
            public Flowable<Boolean> observeFlag(@NonNull Flag flag) {
                return store.observe(key(flag), true);
            }

            @Override
            public Completable setFlag(@NonNull Flag flag, boolean value) {
                return store.put(key(flag), value);
            }

            private androidx.datastore.preferences.core.Preferences.Key<Boolean> key(Flag flag) {
                switch (flag) {
                    case USE_CROPS:
                        return SettingsStore.AI_USE_CROPS;
                    case USE_WEATHER:
                        return SettingsStore.AI_USE_WEATHER;
                    default:
                        return SettingsStore.AI_SAVE_HISTORY;
                }
            }
        };
    }
}
