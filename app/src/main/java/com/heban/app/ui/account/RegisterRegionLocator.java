package com.heban.app.ui.account;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * A06「所在地区」定位：LocationManager 取最近一次位置（没有时请求一次当前位置），
 * 再用系统 Geocoder 反查为「省 市 区」。调用前需已获得 ACCESS_COARSE_LOCATION 权限。
 */
final class RegisterRegionLocator {
    private static final long CURRENT_LOCATION_TIMEOUT_S = 15;

    private final Context app;

    RegisterRegionLocator(Context context) {
        app = context.getApplicationContext();
    }

    /** 返回「江苏省 南京市 江宁区」形式的地区；任何失败都以错误结束 */
    Single<String> locate() {
        return location()
                .flatMap(this::reverseGeocode)
                .timeout(CURRENT_LOCATION_TIMEOUT_S + 10, TimeUnit.SECONDS);
    }

    @SuppressLint("MissingPermission")
    private Single<Location> location() {
        return Single.<Location>create(emitter -> {
            LocationManager lm = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                emitter.onError(failure());
                return;
            }
            Location best = null;
            for (String provider : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            if (best != null) {
                emitter.onSuccess(best);
                return;
            }
            String provider = pickProvider(lm);
            if (provider == null) {
                emitter.onError(failure());
                return;
            }
            CancellationSignal cancel = new CancellationSignal();
            emitter.setCancellable(cancel::cancel);
            lm.getCurrentLocation(provider, cancel, Runnable::run, l -> {
                if (l != null) emitter.onSuccess(l);
                else emitter.onError(failure());
            });
        }).timeout(CURRENT_LOCATION_TIMEOUT_S, TimeUnit.SECONDS);
    }

    @Nullable
    private static String pickProvider(LocationManager lm) {
        String[] candidates = {LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER};
        for (String p : candidates) {
            if (lm.isProviderEnabled(p)) return p;
        }
        return null;
    }

    private Single<String> reverseGeocode(Location loc) {
        if (!Geocoder.isPresent()) return Single.error(failure());
        Geocoder geocoder = new Geocoder(app, Locale.SIMPLIFIED_CHINESE);
        Single<List<Address>> addresses;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addresses = Single.create(emitter -> geocoder.getFromLocation(
                    loc.getLatitude(), loc.getLongitude(), 1, new Geocoder.GeocodeListener() {
                        @Override
                        public void onGeocode(@NonNull List<Address> list) {
                            emitter.onSuccess(list);
                        }

                        @Override
                        public void onError(@Nullable String message) {
                            emitter.onError(failure());
                        }
                    }));
        } else {
            addresses = Single.fromCallable(() -> legacyGeocode(geocoder, loc));
        }
        return addresses
                .subscribeOn(Schedulers.io())
                .map(list -> {
                    String region = list.isEmpty() ? "" : format(list.get(0));
                    if (region.isEmpty()) throw failure();
                    return region;
                });
    }

    @SuppressWarnings("deprecation")
    private static List<Address> legacyGeocode(Geocoder geocoder, Location loc) throws Exception {
        List<Address> list = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
        return list == null ? new ArrayList<>() : list;
    }

    /** 省 市 区，去掉直辖市重复的「北京市 北京市」 */
    static String format(Address a) {
        String[] parts = {a.getAdminArea(), a.getLocality(), a.getSubLocality()};
        StringBuilder sb = new StringBuilder();
        String last = null;
        for (String p : parts) {
            if (p == null || p.trim().isEmpty()) continue;
            String t = p.trim();
            if (t.equals(last)) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
            last = t;
        }
        return sb.toString();
    }

    /** 界面统一提示「定位失败，请手动输入」，这里不需要具体原因 */
    private static IllegalStateException failure() {
        return new IllegalStateException("locate failed");
    }
}
