package com.heban.app.data.remote.sms;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.BuildConfig;
import com.heban.app.data.remote.NetworkModule;

import okhttp3.OkHttpClient;

/** 根据 BuildConfig 选择短信实现：SMS_PROVIDER=aliyun 且配置了 AccessKey 时用阿里云，否则用模拟实现。 */
public final class SmsServices {
    public static final String PROVIDER_ALIYUN = "aliyun";

    private SmsServices() {
    }

    public static SmsService create(OkHttpClient client, Gson gson) {
        if (useAliyun(BuildConfig.SMS_PROVIDER, BuildConfig.ALIYUN_AK_ID, BuildConfig.ALIYUN_AK_SECRET)) {
            AliyunApi api = NetworkModule.retrofit(AliyunSigner.ENDPOINT, client, gson)
                    .create(AliyunApi.class);
            return new AliyunSmsService(api, gson, BuildConfig.ALIYUN_AK_ID,
                    BuildConfig.ALIYUN_AK_SECRET, BuildConfig.ALIYUN_SMS_SIGN_NAME);
        }
        return new FakeSmsService();
    }

    /** SMS_PROVIDER 为 aliyun，且 AccessKey ID / Secret 都已配置 */
    static boolean useAliyun(@Nullable String provider, @Nullable String akId, @Nullable String akSecret) {
        return provider != null && PROVIDER_ALIYUN.equalsIgnoreCase(provider.trim())
                && notBlank(akId) && notBlank(akSecret);
    }

    private static boolean notBlank(@Nullable String s) {
        return s != null && !s.trim().isEmpty();
    }
}
