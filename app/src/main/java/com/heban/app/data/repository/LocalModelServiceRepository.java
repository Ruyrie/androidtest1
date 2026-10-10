package com.heban.app.data.repository;

import com.google.gson.Gson;
import com.heban.app.BuildConfig;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.model.ModelServiceConfig;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.OkHttpClient;

/** ModelServiceRepository 的本地实现（DataStore + SecureStore）。M4 数据层实现前为占位。 */
public final class LocalModelServiceRepository implements ModelServiceRepository {
    private final OkHttpClient baseClient;
    private final Gson gson;
    private final SettingsStore settings;
    private final SecureStore secureStore;

    public LocalModelServiceRepository(OkHttpClient baseClient, Gson gson, SettingsStore settings,
                                       SecureStore secureStore) {
        this.baseClient = baseClient;
        this.gson = gson;
        this.settings = settings;
        this.secureStore = secureStore;
    }

    @Override
    public Flowable<ModelServiceConfig> observeUserConfig() {
        return Flowable.just(new ModelServiceConfig());
    }

    @Override
    public Completable save(ModelServiceConfig config) {
        return Completable.complete();
    }

    @Override
    public Completable clear() {
        return Completable.complete();
    }

    @Override
    public Single<String> test(ModelServiceConfig config) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public boolean hasBuiltInKey() {
        return !BuildConfig.DEEPSEEK_API_KEY.isEmpty();
    }
}
