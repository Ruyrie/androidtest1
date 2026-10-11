package com.heban.app.data.weather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.heban.app.data.local.entity.UserEntity;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.model.CityOption;
import com.heban.app.data.remote.weather.QWeatherApi;
import com.heban.app.data.remote.weather.QWeatherErrors;
import com.heban.app.data.remote.weather.QWeatherException;
import com.heban.app.data.remote.weather.QWeatherMapper;
import com.heban.app.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * 确定天气地点（技术准备文档 8.2 / 权限表）：
 * <ol>
 *   <li>H02 中保存的地点（weather.location_id / weather.location_name）；</li>
 *   <li>设备粗略定位（已授予 ACCESS_COARSE_LOCATION 时，取 NETWORK / PASSIVE 的最后已知位置），
 *       用 GeoAPI 以「经度,纬度」反查 LocationID，显示名如「南京 江宁」；</li>
 *   <li>当前用户资料中的地区（如「江苏省 南京市 江宁区」），从最具体的一级开始用 GeoAPI 搜索；</li>
 *   <li>都没有时抛出 {@link QWeatherErrors#noLocation()}（「请选择地点」）。</li>
 * </ol>
 * 自动解析的结果在进程内按坐标 / 地区缓存，避免每次刷新都请求 GeoAPI。
 */
public class LocationResolver {
    private static final String[] LONG_SUFFIXES = {"特别行政区", "自治区", "自治州", "自治县", "地区"};
    private static final String[] SHORT_SUFFIXES = {"省", "市", "区", "县", "盟", "旗"};

    private final Context app;
    private final SettingsStore settings;
    private final UserRepository userRepository;

    private String autoKey;
    private CityOption autoCity;

    public LocationResolver(Context app, SettingsStore settings, UserRepository userRepository) {
        this.app = app;
        this.settings = settings;
        this.userRepository = userRepository;
    }

    /** 在 io 线程订阅 */
    public Single<CityOption> resolve(QWeatherApi api) {
        return Single.fromCallable(() -> resolveBlocking(api));
    }

    /** 是否保存了手动选择的地点 */
    public boolean hasSavedLocation() {
        return !settings.get(SettingsStore.WEATHER_LOCATION_ID, "").blockingGet().isEmpty();
    }

    CityOption resolveBlocking(QWeatherApi api) {
        String id = settings.get(SettingsStore.WEATHER_LOCATION_ID, "").blockingGet();
        if (!id.isEmpty()) {
            CityOption c = new CityOption();
            c.id = id;
            String name = settings.get(SettingsStore.WEATHER_LOCATION_NAME, "").blockingGet();
            c.name = name.isEmpty() ? id : name;
            return c;
        }
        double[] lonLat = lastKnownLonLat();
        if (lonLat != null) {
            String key = lonLat(lonLat[0], lonLat[1]);
            CityOption c = cached(key);
            if (c == null) {
                List<CityOption> list = lookup(api, key, null);
                if (!list.isEmpty()) c = remember(key, list.get(0));
            }
            if (c != null) return c;
        }
        String region = currentRegion();
        if (region != null && !region.trim().isEmpty()) {
            String key = "region:" + region.trim();
            CityOption c = cached(key);
            if (c == null) {
                for (String[] q : regionQueries(region)) {
                    List<CityOption> list = lookup(api, q[0], q[1]);
                    if (!list.isEmpty()) {
                        c = remember(key, list.get(0));
                        break;
                    }
                }
            }
            if (c != null) return c;
        }
        throw QWeatherErrors.noLocation();
    }

    @Nullable
    private synchronized CityOption cached(String key) {
        return key.equals(autoKey) ? autoCity : null;
    }

    private synchronized CityOption remember(String key, CityOption c) {
        autoKey = key;
        autoCity = c;
        return c;
    }

    /** GeoAPI 城市搜索（range=cn，最多 10 条）；没有结果（旧版 code 404 / 204 或新版 NO SUCH LOCATION）返回空列表 */
    public static Single<List<CityOption>> search(QWeatherApi api, String location, @Nullable String adm) {
        return api.lookupCity(location, adm, "cn", 10)
                .map(resp -> {
                    QWeatherErrors.ensureOk(resp.code);
                    return QWeatherMapper.toCities(resp);
                })
                .onErrorResumeNext(e -> {
                    Throwable t = QWeatherErrors.wrap(e);
                    if (t instanceof QWeatherException
                            && QWeatherErrors.MSG_NO_SUCH_LOCATION.equals(t.getMessage())) {
                        return Single.just(Collections.<CityOption>emptyList());
                    }
                    return Single.error(t);
                });
    }

    static List<CityOption> lookup(QWeatherApi api, String location, @Nullable String adm) {
        return search(api, location, adm).blockingGet();
    }

    /** 粗略定位的最后已知位置 {经度, 纬度}；没有权限或没有位置时返回 null */
    @Nullable
    private double[] lastKnownLonLat() {
        boolean coarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean fine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!coarse && !fine) return null;
        LocationManager lm = app.getSystemService(LocationManager.class);
        if (lm == null) return null;
        for (String provider : new String[]{LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
            try {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null) return new double[]{l.getLongitude(), l.getLatitude()};
            } catch (SecurityException | IllegalArgumentException ignored) {
                // 没有该定位来源或权限被撤销
            }
        }
        return null;
    }

    /** 当前登录用户资料中的地区；游客 / 未登录（不发射）时返回 null */
    @Nullable
    private String currentRegion() {
        try {
            UserEntity u = userRepository.currentUser()
                    .firstElement()
                    .timeout(2, TimeUnit.SECONDS, Maybe.empty())
                    .blockingGet();
            return u == null ? null : u.region;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** GeoAPI 的经纬度参数「经度,纬度」，最多两位小数 */
    public static String lonLat(double lon, double lat) {
        return String.format(Locale.US, "%.2f,%.2f", lon, lat);
    }

    /**
     * 地区 → 依次尝试的 {搜索词, 上级行政区划}：从最具体的一级开始，先原文再去掉「区 / 县 / 市 / 省」等后缀。
     * 「江苏省 南京市 江宁区」→ [江宁区, 南京], [江宁, 南京], [江宁, null], [南京市, 江苏], [南京, 江苏], [江苏省, null], [江苏, null]
     */
    @NonNull
    public static List<String[]> regionQueries(@Nullable String region) {
        List<String[]> out = new ArrayList<>();
        List<String> parts = regionParts(region);
        for (int i = parts.size() - 1; i >= 0; i--) {
            String raw = parts.get(i);
            String bare = stripSuffix(raw);
            String adm = i > 0 ? stripSuffix(parts.get(i - 1)) : null;
            add(out, raw, adm);
            add(out, bare, adm);
            if (i == parts.size() - 1 && adm != null) add(out, bare, null);
        }
        return out;
    }

    static List<String> regionParts(@Nullable String region) {
        List<String> parts = new ArrayList<>();
        if (region == null) return parts;
        for (String p : region.trim().split("[\\s/·,，、]+")) {
            if (!p.isEmpty()) parts.add(p);
        }
        if (parts.size() == 1 && parts.get(0).length() > 4) {
            List<String> split = new ArrayList<>();
            for (String p : parts.get(0).split("(?<=[省市区县州盟旗])")) {
                if (!p.isEmpty()) split.add(p);
            }
            if (split.size() > 1) return split;
        }
        return parts;
    }

    static String stripSuffix(String s) {
        for (String suf : LONG_SUFFIXES) {
            if (s.endsWith(suf) && s.length() - suf.length() >= 2) return s.substring(0, s.length() - suf.length());
        }
        for (String suf : SHORT_SUFFIXES) {
            if (s.endsWith(suf) && s.length() - suf.length() >= 2) return s.substring(0, s.length() - suf.length());
        }
        return s;
    }

    private static void add(List<String[]> out, String q, @Nullable String adm) {
        for (String[] e : out) {
            if (e[0].equals(q) && (e[1] == null ? adm == null : e[1].equals(adm))) return;
        }
        out.add(new String[]{q, adm});
    }
}
