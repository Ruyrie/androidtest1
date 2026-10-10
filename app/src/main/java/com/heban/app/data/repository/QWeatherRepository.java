package com.heban.app.data.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.model.CityOption;
import com.heban.app.data.model.WeatherState;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.OkHttpClient;

/** WeatherRepository 的和风天气实现（技术准备文档 8.2）。M3 数据层实现前为占位。 */
public final class QWeatherRepository implements WeatherRepository {
    private final Context app;
    private final OkHttpClient baseClient;
    private final Gson gson;
    private final AppDatabase db;
    private final SettingsStore settings;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;

    public QWeatherRepository(Context app, OkHttpClient baseClient, Gson gson, AppDatabase db,
                              SettingsStore settings, UserRepository userRepository,
                              CropRepository cropRepository) {
        this.app = app;
        this.baseClient = baseClient;
        this.gson = gson;
        this.db = db;
        this.settings = settings;
        this.userRepository = userRepository;
        this.cropRepository = cropRepository;
    }

    @Override
    public Flowable<WeatherState> observe() {
        WeatherState s = new WeatherState();
        s.loading = true;
        return Flowable.just(s);
    }

    @Override
    public Completable refresh(boolean force) {
        return Completable.complete();
    }

    @Override
    public Single<List<CityOption>> searchCity(String keyword) {
        return Single.just(Collections.emptyList());
    }

    @Override
    public Completable setLocation(CityOption city) {
        return Completable.complete();
    }
}
