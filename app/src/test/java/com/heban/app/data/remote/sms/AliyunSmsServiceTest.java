package com.heban.app.data.remote.sms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.heban.app.BuildConfig;
import com.heban.app.data.remote.BusinessException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

public class AliyunSmsServiceTest {
    private final List<String> urls = new ArrayList<>();
    private AliyunResp next;
    private Throwable nextError;
    private AliyunSmsService service;

    @Before
    public void setUp() {
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        AliyunApi api = url -> {
            urls.add(url);
            return nextError != null ? Single.error(nextError) : Single.just(next);
        };
        service = new AliyunSmsService(api, new Gson(), "testid", "testsecret", "禾伴");
    }

    @After
    public void tearDown() {
        RxJavaPlugins.reset();
    }

    @Test
    public void send_buildsSendSmsVerifyCodeRequest() {
        next = resp("OK", "OK", null);
        service.send("13800005678", SmsScene.RESET_PASSWORD).test().assertComplete();

        Map<String, String> q = query(urls.get(0));
        assertEquals("SendSmsVerifyCode", q.get("Action"));
        assertEquals("13800005678", q.get("PhoneNumber"));
        assertEquals("86", q.get("CountryCode"));
        assertEquals("禾伴", q.get("SignName"));
        assertEquals(BuildConfig.ALIYUN_SMS_TPL_RESET, q.get("TemplateCode"));
        assertEquals("{\"code\":\"##code##\",\"min\":\"5\"}", q.get("TemplateParam"));
        assertEquals("6", q.get("CodeLength"));
        assertEquals("300", q.get("ValidTime"));
        assertEquals("60", q.get("Interval"));
        assertEquals("1", q.get("CodeType"));
        assertEquals("testid", q.get("AccessKeyId"));
        assertTrue(q.containsKey("Signature"));
    }

    @Test
    public void send_nonOkCode_throwsBusinessExceptionWithMessage() {
        next = resp("isv.BUSINESS_LIMIT_CONTROL", "触发分钟级流控", null);
        service.send("13800005678", SmsScene.LOGIN).test()
                .assertError(e -> e instanceof BusinessException
                        && "isv.BUSINESS_LIMIT_CONTROL".equals(((BusinessException) e).code)
                        && "触发分钟级流控".equals(e.getMessage()));
    }

    @Test
    public void send_httpErrorWithAliyunBody_throwsBusinessException() {
        String body = "{\"Code\":\"SignatureDoesNotMatch\",\"Message\":\"Specified signature is not matched.\","
                + "\"RequestId\":\"r1\"}";
        nextError = new HttpException(Response.error(400,
                ResponseBody.create(body, MediaType.get("application/json"))));
        service.send("13800005678", SmsScene.LOGIN).test()
                .assertError(e -> e instanceof BusinessException
                        && "SignatureDoesNotMatch".equals(((BusinessException) e).code));
    }

    @Test
    public void verify_passOnlyWhenOkAndVerifyResultPass() {
        next = resp("OK", "OK", "PASS");
        service.verify("13800005678", "123456").test().assertValue(true);
        Map<String, String> q = query(urls.get(0));
        assertEquals("CheckSmsVerifyCode", q.get("Action"));
        assertEquals("123456", q.get("VerifyCode"));
        assertEquals("86", q.get("CountryCode"));

        next = resp("OK", "OK", "UNKNOWN");
        service.verify("13800005678", "000000").test().assertValue(false);

        next = resp("isv.ValidateFail", "校验失败", null);
        service.verify("13800005678", "000000").test().assertValue(false);
    }

    @Test
    public void isFake_false() {
        assertFalse(service.isFake());
    }

    private static AliyunResp resp(String code, String message, String verifyResult) {
        AliyunResp r = new AliyunResp();
        r.code = code;
        r.message = message;
        r.requestId = "req";
        if (verifyResult != null) {
            r.model = new AliyunResp.Model();
            r.model.verifyResult = verifyResult;
        }
        return r;
    }

    private static Map<String, String> query(String url) {
        Map<String, String> m = new HashMap<>();
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            int i = pair.indexOf('=');
            m.put(pair.substring(0, i), decode(pair.substring(i + 1)));
        }
        return m;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
