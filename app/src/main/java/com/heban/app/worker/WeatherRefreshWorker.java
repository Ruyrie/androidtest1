package com.heban.app.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.heban.app.HeBanApp;
import com.heban.app.data.model.WeatherAlert;
import com.heban.app.data.model.WeatherState;
import com.heban.app.di.AppContainer;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * 后台刷新天气（技术准备文档表 11-1）：每 3 小时、需要联网。刷新缓存（30 分钟内的缓存不重复请求）；
 * 满足降温预警规则时通过「天气预警」渠道通知（不受免打扰限制），同一条预警每天只通知一次。
 */
public class WeatherRefreshWorker extends RxWorker {
    public static final String UNIQUE_NAME = "weather-refresh";
    /** 最近一次通知的预警：「日期|标题|内容」 */
    static final Preferences.Key<String> LAST_ALERT = PreferencesKeys.stringKey("weather.last_alert");

    public WeatherRefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Single<Result> createWork() {
        AppContainer c = HeBanApp.container(getApplicationContext());
        return c.weatherRepository.refresh(false)
                .andThen(c.weatherRepository.observe()
                        .filter(s -> !s.loading)
                        .firstOrError()
                        .timeout(60, TimeUnit.SECONDS))
                .flatMap(s -> notifyIfNeeded(c, s).andThen(Single.just(Result.success())))
                .onErrorReturnItem(Result.success());
    }

    private Completable notifyIfNeeded(AppContainer c, WeatherState s) {
        WeatherAlert alert = s.alert;
        if (alert == null) return Completable.complete();
        String key = alertKey(LocalDate.now(), alert);
        return c.settings.get(LAST_ALERT, "")
                .flatMapCompletable(last -> key.equals(last) ? Completable.complete()
                        : c.notifier.showWeatherAlert(alert).andThen(c.settings.put(LAST_ALERT, key)));
    }

    /** 同一天、同一条预警只通知一次 */
    static String alertKey(LocalDate day, @Nullable WeatherAlert alert) {
        if (alert == null) return "";
        return day + "|" + alert.title + "|" + alert.detailText;
    }

    /** 在 Application 启动时调用：唯一周期任务 weather-refresh（KEEP） */
    public static void enqueue(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(WeatherRefreshWorker.class, 3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, req);
    }
}
