package com.heban.app.data.remote;

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import retrofit2.HttpException;

/** 所有异常统一转换为 AppError，界面只显示其中的中文提示（技术准备文档表 5-3）。 */
public final class AppError {
    public enum Type { NETWORK, TIMEOUT, UNAUTHORIZED, NO_BALANCE, RATE_LIMIT, SERVER, PARSE, BUSINESS, UNKNOWN }

    public final Type type;
    public final int httpCode;
    @NonNull public final String message;

    public AppError(Type type, int httpCode, @NonNull String message) {
        this.type = type;
        this.httpCode = httpCode;
        this.message = message;
    }

    public static AppError from(Throwable t) {
        if (t instanceof BusinessException) {
            String m = t.getMessage();
            return new AppError(Type.BUSINESS, 0, m == null ? "操作失败，请重试" : m);
        }
        if (t instanceof HttpException) {
            return fromHttp(((HttpException) t).code());
        }
        if (t instanceof HttpCodeException) {
            return fromHttp(((HttpCodeException) t).code);
        }
        if (t instanceof SocketTimeoutException || t instanceof TimeoutException) {
            return new AppError(Type.TIMEOUT, 0, "请求超时，请稍后重试");
        }
        if (t instanceof IOException) {
            return new AppError(Type.NETWORK, 0, "网络不太好，检查后再试一次");
        }
        if (t instanceof JsonParseException) {
            return new AppError(Type.PARSE, 0, "返回内容无法识别");
        }
        return new AppError(Type.UNKNOWN, 0, "出了点问题，请重试");
    }

    public static AppError fromHttp(int code) {
        if (code == 401) return new AppError(Type.UNAUTHORIZED, code, "API Key 无效，请到「AI 助手设置 → 模型服务」检查");
        if (code == 402) return new AppError(Type.NO_BALANCE, code, "模型服务余额不足");
        if (code == 429) return new AppError(Type.RATE_LIMIT, code, "操作太频繁，请稍后再试");
        if (code >= 500) return new AppError(Type.SERVER, code, "服务暂时不可用，请稍后重试");
        return new AppError(Type.UNKNOWN, code, "出了点问题，请重试");
    }

    /** 不经过 Retrofit 的请求（如 OkHttp SSE）返回的 HTTP 错误 */
    public static class HttpCodeException extends IOException {
        public final int code;

        public HttpCodeException(int code, String message) {
            super(message);
            this.code = code;
        }
    }
}
