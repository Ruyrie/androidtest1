package com.heban.app.data.remote;

import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/** 共用的 OkHttpClient 与 Retrofit 构建（技术准备文档 8.1）。 */
public final class NetworkModule {
    private NetworkModule() {
    }

    public static OkHttpClient baseClient(boolean debug) {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(debug ? HttpLoggingInterceptor.Level.BASIC
                           : HttpLoggingInterceptor.Level.NONE);
        log.redactHeader("Authorization");
        log.redactHeader("X-QW-Api-Key");
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(log)
                .build();
    }

    public static Retrofit retrofit(String baseUrl, OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(
                        RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();
    }
}
