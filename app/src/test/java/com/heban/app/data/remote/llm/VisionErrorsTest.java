package com.heban.app.data.remote.llm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonSyntaxException;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.BusinessException;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

/** 表 9-4：是否切换备用模型 */
public class VisionErrorsTest {

    @Test
    public void timeoutAndNetwork_fallback() {
        assertTrue(VisionErrors.shouldFallback(new TimeoutException()));
        assertTrue(VisionErrors.shouldFallback(new SocketTimeoutException()));
        assertTrue(VisionErrors.shouldFallback(new IOException("reset")));
        assertTrue(VisionErrors.shouldFallback(new UnknownHostException("api.deepseek.com")));
    }

    @Test
    public void http400_404_5xx_fallback() {
        assertTrue(VisionErrors.shouldFallback(new LlmException(400, "bad")));
        assertTrue(VisionErrors.shouldFallback(new LlmException(404, "model not found")));
        assertTrue(VisionErrors.shouldFallback(new LlmException(500, null)));
        assertTrue(VisionErrors.shouldFallback(new LlmException(503, null)));
        assertTrue(VisionErrors.shouldFallback(new AppError.HttpCodeException(502, "gateway")));
        assertTrue(VisionErrors.shouldFallback(retrofitError(404)));
    }

    @Test
    public void invalidJson_fallback() {
        assertTrue(VisionErrors.shouldFallback(new ScanResultParser.ScanParseException("x")));
        assertTrue(VisionErrors.shouldFallback(new JsonSyntaxException("x")));
    }

    @Test
    public void keyBalanceRate_noFallback() {
        assertFalse(VisionErrors.shouldFallback(new LlmException(401, null)));
        assertFalse(VisionErrors.shouldFallback(new LlmException(403, null)));
        assertFalse(VisionErrors.shouldFallback(new LlmException(402, null)));
        assertFalse(VisionErrors.shouldFallback(new LlmException(429, null)));
        assertFalse(VisionErrors.shouldFallback(retrofitError(401)));
    }

    @Test
    public void businessAndUnknown_noFallback() {
        assertFalse(VisionErrors.shouldFallback(LlmConfigProvider.notConfigured()));
        assertFalse(VisionErrors.shouldFallback(new BusinessException("NOT_LOGGED_IN", "请先登录")));
        assertFalse(VisionErrors.shouldFallback(null));
    }

    private static HttpException retrofitError(int code) {
        return new HttpException(Response.error(code,
                ResponseBody.create("{}", MediaType.get("application/json"))));
    }
}
