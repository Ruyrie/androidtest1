package com.heban.app.data.remote.llm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.llm.dto.ChatChunk;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.HttpException;

/**
 * 模型服务返回的 HTTP 错误（技术准备文档表 9-6）。继承 AppError.HttpCodeException，
 * 所以 AppError.from 直接给出对应的中文提示（401 检查 Key、402 余额不足、429 稍后再试、5xx 服务不可用）。
 * serverMessage 为服务返回的 error.message，只用于日志。
 */
public class LlmException extends AppError.HttpCodeException {
    @Nullable public final String serverMessage;

    public LlmException(int code, @Nullable String serverMessage) {
        super(code, "HTTP " + code + (serverMessage == null || serverMessage.isEmpty() ? "" : ": " + serverMessage));
        this.serverMessage = serverMessage;
    }

    /**
     * SSE 的 onFailure 转换：有失败的 HTTP 响应时转为 LlmException（读取错误体）；
     * 否则返回原异常（IOException 等，由 AppError 转为网络 / 超时提示）。
     */
    @NonNull
    public static Throwable from(@Nullable Throwable t, @Nullable Response r) {
        if (r != null && !r.isSuccessful()) {
            return new LlmException(r.code(), readMessage(r.body()));
        }
        if (t != null) return t;
        return new IOException("连接中断");
    }

    /** Retrofit 的 HttpException 转为 LlmException；其他异常原样返回 */
    @NonNull
    public static Throwable fromRetrofit(@NonNull Throwable t) {
        if (t instanceof HttpException) {
            HttpException h = (HttpException) t;
            retrofit2.Response<?> resp = h.response();
            return new LlmException(h.code(), resp == null ? null : readMessage(resp.errorBody()));
        }
        return t;
    }

    /** 200 响应或流中出现的 {"error": {...}} */
    @NonNull
    public static LlmException fromErrorBody(@NonNull ChatChunk.ErrorBody error) {
        int code = 0;
        if (error.code instanceof Number) code = ((Number) error.code).intValue();
        else if (error.code instanceof String) {
            try {
                code = Integer.parseInt(((String) error.code).trim());
            } catch (NumberFormatException ignored) {
                // 非数字的错误码（如 "invalid_api_key"）
            }
        }
        if (code < 400 || code > 599) code = 500;
        return new LlmException(code, error.message);
    }

    /** 解析错误体中的 error.message（OpenAI 兼容格式），失败时返回截断的原文 */
    @Nullable
    static String readMessage(@Nullable ResponseBody body) {
        if (body == null) return null;
        try {
            return parseMessage(body.string());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    @Nullable
    static String parseMessage(@Nullable String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            ChatChunk c = new Gson().fromJson(text, ChatChunk.class);
            if (c != null && c.error != null && c.error.message != null) return c.error.message;
        } catch (JsonParseException ignored) {
            // 不是 JSON
        }
        String t = text.trim();
        return t.length() > 200 ? t.substring(0, 200) : t;
    }
}
