package com.heban.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.BuildConfig;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.model.CityOption;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.model.WeatherState;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.weather.QWeatherApi;
import com.heban.app.data.remote.weather.QWeatherApis;
import com.heban.app.data.remote.weather.QWeatherErrors;
import com.heban.app.data.remote.weather.QWeatherMapper;
import com.heban.app.data.weather.AttentionRules;
import com.heban.app.data.weather.FarmIndexCalculator;
import com.heban.app.data.weather.LocationResolver;
import com.heban.app.data.weather.WeatherAlertRules;
import com.heban.app.data.weather.WeatherCacheMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.processors.FlowableProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;

/**
 * WeatherRepository 的和风天气实现（技术准备文档 8.2）。
 *
 * <p>H01 / H02 共用一个状态流（BehaviorProcessor）：订阅时先发射当前状态（首次为 loading），
 * 然后读取地点与 weather_cache 缓存并发射；缓存超过 30 分钟（或没有缓存）时用 Single.zip 并发请求
 * 实时 / 逐小时 / 7 天预报，写入缓存后发射最新状态。请求失败时保留缓存并标记 offline，error 为中文提示。
 * 未配置 API Host / Key 时只发射带 error 的状态。每次取到新天气后按 {@link AttentionRules} 更新作物「需关注」。
 *
 * <p>{@link #refresh(boolean)} 在本次加载结束后完成（错误通过状态中的 error 显示，不会让 Completable 失败）。
 */
public final class QWeatherRepository implements WeatherRepository {
    /** 30 分钟内不重复请求 */
    public static final long CACHE_TTL = 30 * 60_000L;
    /** 自动加载（打开页面）失败后 1 分钟内不再自动请求（下拉刷新不受限） */
    static final long ERROR_BACKOFF = 60_000L;

    private enum Mode { AUTO, REFRESH, LOCATION, FORCE }

    private final Context app;
    private final OkHttpClient baseClient;
    private final Gson gson;
    private final AppDatabase db;
    private final SettingsStore settings;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;

    @Nullable private final QWeatherApi api;
    private final LocationResolver locationResolver;
    private final LongSupplier clock;
    @Nullable private final ZoneId fixedZone;

    private final BehaviorProcessor<WeatherState> subject = BehaviorProcessor.createDefault(initialState());
    private final FlowableProcessor<WeatherState> state = subject.toSerialized();

    private final Object lock = new Object();
    @Nullable private Completable running;
    @Nullable private Mode runningMode;
    private volatile long lastFailureAt;

    public QWeatherRepository(Context app, OkHttpClient baseClient, Gson gson, AppDatabase db,
                              SettingsStore settings, UserRepository userRepository,
                              CropRepository cropRepository) {
        this(app, baseClient, gson, db, settings, userRepository, cropRepository,
                BuildConfig.QWEATHER_API_HOST, BuildConfig.QWEATHER_API_KEY, System::currentTimeMillis, null);
    }

    /** 测试用：指定 API Host / Key、时钟与时区（zone 为 null 时用系统时区） */
    public QWeatherRepository(Context app, OkHttpClient baseClient, Gson gson, AppDatabase db,
                              SettingsStore settings, UserRepository userRepository,
                              CropRepository cropRepository, @Nullable String apiHost, @Nullable String apiKey,
                              LongSupplier clock, @Nullable ZoneId zone) {
        this.app = app;
        this.baseClient = baseClient;
        this.gson = gson;
        this.db = db;
        this.settings = settings;
        this.userRepository = userRepository;
        this.cropRepository = cropRepository;
        this.clock = clock;
        this.fixedZone = zone;
        this.api = QWeatherApis.isConfigured(apiHost, apiKey)
                ? QWeatherApis.create(baseClient, gson, apiHost, apiKey) : null;
        this.locationResolver = new LocationResolver(app, settings, userRepository);
    }

    @Override
    public Flowable<WeatherState> observe() {
        return Flowable.defer(() -> {
            submit(Mode.AUTO);
            return state.onBackpressureLatest();
        });
    }

    @Override
    public Completable refresh(boolean force) {
        return Completable.defer(() -> submit(force ? Mode.FORCE : Mode.REFRESH));
    }

    @Override
    public Single<List<CityOption>> searchCity(String keyword) {
        return Single.defer(() -> {
            if (api == null) return Single.error(QWeatherErrors.notConfigured());
            String k = keyword == null ? "" : keyword.trim();
            if (k.isEmpty()) return Single.just(Collections.<CityOption>emptyList());
            return LocationResolver.search(api, k, null);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable setLocation(CityOption city) {
        if (city == null || city.id == null || city.id.isEmpty()) {
            return Completable.error(QWeatherErrors.noLocation());
        }
        String name = city.displayName() == null ? city.id : city.displayName();
        return settings.put(SettingsStore.WEATHER_LOCATION_ID, city.id)
                .andThen(settings.put(SettingsStore.WEATHER_LOCATION_NAME, name))
                .andThen(Completable.defer(() -> submit(Mode.LOCATION)))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String> farmTaskTip(FarmType type, LocalDate day) {
        return Single.fromCallable(() -> {
            WeatherState s = subject.getValue();
            WeatherBundle b = s == null ? null : s.bundle;
            if (b == null) b = WeatherCacheMapper.fromEntity(db.weatherDao().latest().blockingGet(), gson);
            return FarmIndexCalculator.farmTaskTip(b, type, day, clock.getAsLong(), zone());
        }).subscribeOn(Schedulers.io()).onErrorReturnItem("");
    }

    // ———————————————————————— 加载 ————————————————————————

    /**
     * 同一时间只有一次加载：较弱的请求加入正在进行的加载；更强的请求（下拉强制刷新、切换地点）排在其后。
     */
    private Completable submit(Mode mode) {
        synchronized (lock) {
            Completable cur = running;
            if (cur != null) {
                boolean join = mode != Mode.LOCATION && runningMode != null
                        && mode.ordinal() <= runningMode.ordinal();
                if (join) return cur;
                return cur.onErrorComplete().andThen(Completable.defer(() -> submit(mode)));
            }
            Completable[] holder = new Completable[1];
            Completable c = Completable.fromAction(() -> doLoad(mode))
                    .subscribeOn(Schedulers.io())
                    .doFinally(() -> {
                        synchronized (lock) {
                            if (running == holder[0]) {
                                running = null;
                                runningMode = null;
                            }
                        }
                    })
                    .cache();
            holder[0] = c;
            running = c;
            runningMode = mode;
            c.subscribe(() -> { }, e -> { });
            return c;
        }
    }

    private void doLoad(Mode mode) {
        long now = clock.getAsLong();
        WeatherState cur = subject.getValue();
        if (api == null) {
            publish(errorState(QWeatherErrors.notConfigured().toAppError()));
            return;
        }
        if (mode == Mode.AUTO && cur != null && !cur.loading) {
            if (cur.bundle != null && cur.error == null && isFresh(cur.bundle, now) && matchesSavedLocation(cur.bundle)) {
                return;
            }
            if (now - lastFailureAt < ERROR_BACKOFF) return;
        }
        publish(loadingFrom(cur, cur == null ? null : cur.bundle));

        CityOption city;
        try {
            city = locationResolver.resolve(api).blockingGet();
        } catch (RuntimeException e) {
            lastFailureAt = now;
            AppError err = QWeatherErrors.toAppError(e);
            WeatherBundle fallback = QWeatherErrors.isNoLocation(err) ? null : readLatestCache();
            publish(fallback != null ? build(fallback, false, true, err) : errorState(err));
            return;
        }
        String name = city.displayName() == null ? city.id : city.displayName();

        WeatherBundle cached = readCache(city.id);
        if (cached != null) {
            cached.locationName = name;
            if (mode != Mode.FORCE && isFresh(cached, now)) {
                publish(build(cached, false, false, null));
                return;
            }
            publish(build(cached, true, false, null));
        } else if (cur != null && cur.bundle != null && !city.id.equals(cur.bundle.locationId)) {
            publish(loadingFrom(cur, null));   // 换了地点：不再显示旧地点的数据
        }

        WeatherBundle fresh;
        try {
            fresh = fetch(city.id, name, now);
        } catch (RuntimeException e) {
            lastFailureAt = now;
            AppError err = QWeatherErrors.toAppError(e);
            publish(cached != null ? build(cached, false, true, err) : errorState(err));
            return;
        }
        lastFailureAt = 0;
        try {
            db.weatherDao().upsert(WeatherCacheMapper.toEntity(fresh, gson)).blockingAwait();
        } catch (RuntimeException ignored) {
            // 缓存写入失败不影响显示
        }
        publish(build(fresh, false, false, null));
        updateAttention(fresh);
    }

    private WeatherBundle fetch(String id, String name, long now) {
        QWeatherApi a = api;
        return Single.zip(
                        a.now(id).map(r -> {
                            QWeatherErrors.ensureOk(r.code);
                            return r;
                        }),
                        a.hourly(id).map(r -> {
                            QWeatherErrors.ensureOk(r.code);
                            return r;
                        }),
                        a.daily(id).map(r -> {
                            QWeatherErrors.ensureOk(r.code);
                            return r;
                        }),
                        (n, h, d) -> QWeatherMapper.toBundle(n, h, d, id, name, now, zone()))
                .onErrorResumeNext(e -> Single.error(QWeatherErrors.wrap(e)))
                .blockingGet();
    }

    /** 拿到新天气后更新作物「需关注」（天气来源），单个作物出错时忽略 */
    private void updateAttention(WeatherBundle b) {
        List<CropEntity> crops;
        try {
            crops = cropRepository.listCrops().blockingGet();
        } catch (RuntimeException e) {
            return;
        }
        if (crops == null) return;
        long now = clock.getAsLong();
        for (CropEntity crop : crops) {
            try {
                PlotEntity plot = null;
                if (crop.plotId != null) {
                    try {
                        plot = db.plotDao().get(crop.plotId).blockingGet();
                    } catch (RuntimeException ignored) {
                        // 地块已删除
                    }
                }
                String note = AttentionRules.evaluate(crop, plot, b, now, zone());
                cropRepository.setWeatherAttention(crop.id, note).blockingAwait();
            } catch (RuntimeException ignored) {
                // 忽略
            }
        }
    }

    // ———————————————————————— 状态 ————————————————————————

    private static WeatherState initialState() {
        WeatherState s = new WeatherState();
        s.loading = true;
        return s;
    }

    private WeatherState build(WeatherBundle b, boolean loading, boolean offline, @Nullable AppError error) {
        long now = clock.getAsLong();
        ZoneId zone = zone();
        WeatherState s = new WeatherState();
        s.loading = loading;
        s.bundle = b;
        s.indices = FarmIndexCalculator.indices(b, now, zone);
        s.alert = WeatherAlertRules.evaluate(b, now, zone);
        s.farmTip = FarmIndexCalculator.farmTip(b, now, zone);
        s.offline = offline;
        s.error = error;
        return s;
    }

    private static WeatherState errorState(@NonNull AppError error) {
        WeatherState s = new WeatherState();
        s.error = error;
        return s;
    }

    private WeatherState loadingFrom(@Nullable WeatherState cur, @Nullable WeatherBundle bundle) {
        if (bundle == null) return initialState();
        WeatherState s = new WeatherState();
        s.loading = true;
        s.bundle = bundle;
        if (cur != null && cur.bundle == bundle) {
            s.indices = cur.indices;
            s.alert = cur.alert;
            s.farmTip = cur.farmTip;
            s.offline = cur.offline;
        }
        return s;
    }

    private void publish(WeatherState s) {
        state.onNext(s);
    }

    private boolean isFresh(WeatherBundle b, long now) {
        return b.fetchedAt <= now && now - b.fetchedAt < CACHE_TTL;
    }

    /** 当前数据是否属于已保存的地点（没有保存地点时视为一致） */
    private boolean matchesSavedLocation(WeatherBundle b) {
        String saved = settings.get(SettingsStore.WEATHER_LOCATION_ID, "").blockingGet();
        return saved.isEmpty() || saved.equals(b.locationId);
    }

    @Nullable
    private WeatherBundle readCache(String locationId) {
        try {
            return WeatherCacheMapper.fromEntity(db.weatherDao().get(locationId).blockingGet(), gson);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private WeatherBundle readLatestCache() {
        try {
            return WeatherCacheMapper.fromEntity(db.weatherDao().latest().blockingGet(), gson);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private ZoneId zone() {
        return fixedZone != null ? fixedZone : ZoneId.systemDefault();
    }
}
