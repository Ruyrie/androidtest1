package com.heban.app.testutil;

import com.heban.app.data.remote.sms.FakeSmsService;
import com.heban.app.data.remote.sms.SmsScene;
import com.heban.app.data.remote.sms.SmsService;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/** 同步的模拟短信：记录发送记录，验证码固定为 123456 */
public class RecordingSmsService implements SmsService {
    public final List<String> sent = new ArrayList<>();
    public final List<SmsScene> scenes = new ArrayList<>();

    @Override
    public Completable send(String phone, SmsScene scene) {
        return Completable.fromAction(() -> {
            sent.add(phone);
            scenes.add(scene);
        });
    }

    @Override
    public Single<Boolean> verify(String phone, String code) {
        return Single.fromCallable(() -> FakeSmsService.CODE.equals(code));
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
