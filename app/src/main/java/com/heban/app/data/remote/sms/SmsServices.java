package com.heban.app.data.remote.sms;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;

/** 根据 BuildConfig 选择短信实现：SMS_PROVIDER=aliyun 且配置了 AccessKey 时用阿里云，否则用模拟实现。 */
public final class SmsServices {
    private SmsServices() {
    }

    public static SmsService create(OkHttpClient client, Gson gson) {
        // M1：阿里云实现由 AliyunSmsService 提供（见技术准备文档 8.3）
        return new FakeSmsService();
    }
}
