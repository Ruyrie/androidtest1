package com.heban.app.data.remote.sms;

import android.util.Log;

import com.google.gson.Gson;
import com.heban.app.data.remote.BusinessException;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * 阿里云号码认证服务 · 短信认证（技术准备文档 8.3）：SendSmsVerifyCode 发送、CheckSmsVerifyCode 校验，
 * 验证码由阿里云生成和校验。返回 Code 不为 OK 时抛出 BusinessException(Code, Message)，并记录 Code 便于排查。
 */
public final class AliyunSmsService implements SmsService {
    private static final String TAG = "AliyunSms";
    static final String ACTION_SEND = "SendSmsVerifyCode";
    static final String ACTION_CHECK = "CheckSmsVerifyCode";
    static final String TEMPLATE_PARAM = "{\"code\":\"##code##\",\"min\":\"5\"}";
    private static final String FALLBACK_MESSAGE = "短信服务暂时不可用，请稍后重试";

    private final AliyunApi api;
    private final Gson gson;
    private final String akId;
    private final String akSecret;
    private final String signName;

    public AliyunSmsService(AliyunApi api, Gson gson, String akId, String akSecret, String signName) {
        this.api = api;
        this.gson = gson;
        this.akId = akId;
        this.akSecret = akSecret;
        this.signName = signName;
    }

    @Override
    public Completable send(String phone, SmsScene scene) {
        return Single.fromCallable(() -> AliyunSigner.signedUrl(ACTION_SEND,
                        sendParams(phone, scene, signName), akId, akSecret))
                .flatMap(url -> call(ACTION_SEND, url))
                .flatMapCompletable(r -> r.isOk() ? Completable.complete()
                        : Completable.error(toBusiness(ACTION_SEND, r)))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Boolean> verify(String phone, String code) {
        return Single.fromCallable(() -> AliyunSigner.signedUrl(ACTION_CHECK,
                        checkParams(phone, code), akId, akSecret))
                .flatMap(url -> call(ACTION_CHECK, url))
                .map(r -> {
                    if (!r.isOk()) log(ACTION_CHECK, r);
                    return r.isOk() && r.model != null
                            && AliyunResp.VERIFY_PASS.equals(r.model.verifyResult);
                })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public boolean isFake() {
        return false;
    }

    /** SendSmsVerifyCode 业务参数（表 8-5） */
    static Map<String, String> sendParams(String phone, SmsScene scene, String signName) {
        Map<String, String> m = new HashMap<>();
        m.put("PhoneNumber", phone);
        m.put("CountryCode", "86");
        m.put("SignName", signName);
        m.put("TemplateCode", scene.templateCode());
        m.put("TemplateParam", TEMPLATE_PARAM);
        m.put("CodeLength", "6");
        m.put("ValidTime", "300");
        m.put("Interval", "60");
        m.put("CodeType", "1");
        return m;
    }

    /** CheckSmsVerifyCode 业务参数（表 8-5） */
    static Map<String, String> checkParams(String phone, String code) {
        Map<String, String> m = new HashMap<>();
        m.put("PhoneNumber", phone);
        m.put("CountryCode", "86");
        m.put("VerifyCode", code);
        return m;
    }

    /** 签名错误、AccessKey 无效等以 HTTP 4xx 返回，返回体同样带 Code / Message，转换为 BusinessException */
    private Single<AliyunResp> call(String action, String url) {
        return api.call(url).onErrorResumeNext(e -> {
            AliyunResp body = parseErrorBody(e);
            return Single.error(body != null && body.code != null ? toBusiness(action, body) : e);
        });
    }

    private AliyunResp parseErrorBody(Throwable e) {
        if (!(e instanceof HttpException)) return null;
        Response<?> resp = ((HttpException) e).response();
        ResponseBody body = resp == null ? null : resp.errorBody();
        if (body == null) return null;
        try {
            return gson.fromJson(body.string(), AliyunResp.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private BusinessException toBusiness(String action, AliyunResp r) {
        log(action, r);
        String message = r.message == null || r.message.trim().isEmpty() ? FALLBACK_MESSAGE : r.message;
        return new BusinessException(r.code == null ? "SMS_ERROR" : r.code, message);
    }

    private static void log(String action, AliyunResp r) {
        Log.w(TAG, action + " 失败：Code=" + r.code + "，Message=" + r.message
                + "，RequestId=" + r.requestId);
    }
}
