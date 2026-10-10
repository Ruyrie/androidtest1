package com.heban.app.data.remote.sms;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/** 模拟短信：SMS_PROVIDER=fake 或未配置 AccessKey 时使用，验证码固定为 123456，不产生费用。 */
public final class FakeSmsService implements SmsService {
    public static final String CODE = "123456";

    @Override
    public Completable send(String phone, SmsScene scene) {
        return Completable.timer(300, TimeUnit.MILLISECONDS);
    }

    @Override
    public Single<Boolean> verify(String phone, String code) {
        return Single.just(CODE.equals(code)).delay(200, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
