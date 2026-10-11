package com.heban.app.data.remote.weather;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/** 给每个和风天气请求加上 X-QW-Api-Key 请求头（日志中已隐藏该请求头）。 */
public final class QWeatherKeyInterceptor implements Interceptor {
    public static final String HEADER = "X-QW-Api-Key";

    private final String key;

    public QWeatherKeyInterceptor(String key) {
        this.key = key;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder().header(HEADER, key).build());
    }
}
