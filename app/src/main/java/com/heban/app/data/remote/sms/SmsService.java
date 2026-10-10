package com.heban.app.data.remote.sms;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/** 短信验证码：AliyunSmsService（号码认证服务 · 短信认证）或 FakeSmsService（技术准备文档 8.3）。 */
public interface SmsService {
    /** 发送验证码；失败时抛出 BusinessException（message 可直接显示） */
    Completable send(String phone, SmsScene scene);

    /** 校验验证码，true 表示通过 */
    Single<Boolean> verify(String phone, String code);

    /** 是否为模拟模式（固定验证码 123456，界面提示「模拟短信：123456」） */
    boolean isFake();
}
