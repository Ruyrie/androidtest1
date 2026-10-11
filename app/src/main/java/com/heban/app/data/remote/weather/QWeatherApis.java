package com.heban.app.data.remote.weather;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.remote.NetworkModule;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/** 创建 QWeatherApi（技术准备文档 8.1：从共用的 OkHttpClient 派生，读超时 15 秒，请求头带 Key）。 */
public final class QWeatherApis {
    private QWeatherApis() {
    }

    /** host 与 key 都已填写 */
    public static boolean isConfigured(@Nullable String host, @Nullable String key) {
        return baseUrl(host) != null && key != null && !key.trim().isEmpty();
    }

    /**
     * 控制台中的 API Host（如 abcd.re.qweatherapi.com）→ Retrofit Base URL：
     * 没有协议时补 https://，末尾补 /。未填写时返回 null。
     */
    @Nullable
    public static String baseUrl(@Nullable String host) {
        if (host == null) return null;
        String h = host.trim();
        if (h.isEmpty()) return null;
        if (!h.startsWith("http://") && !h.startsWith("https://")) h = "https://" + h;
        if (!h.endsWith("/")) h = h + "/";
        return h;
    }

    public static QWeatherApi create(OkHttpClient baseClient, Gson gson, String host, String key) {
        String url = baseUrl(host);
        if (url == null) throw new IllegalArgumentException("QWEATHER_API_HOST is empty");
        OkHttpClient client = baseClient.newBuilder()
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(new QWeatherKeyInterceptor(key.trim()))
                .build();
        return NetworkModule.retrofit(url, client, gson).create(QWeatherApi.class);
    }
}
