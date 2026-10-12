package com.heban.app.data.remote.llm;

import androidx.annotation.Nullable;

import com.google.gson.JsonParseException;
import com.heban.app.data.remote.AppError;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import retrofit2.HttpException;

/**
 * 主模型失败时是否切换备用模型（技术准备文档表 9-4）：
 * <ul>
 *   <li>超时、网络错误（IOException）→ 切换；</li>
 *   <li>HTTP 400 / 404（实验版模型可能改名、下线或不支持图片）、5xx → 切换；</li>
 *   <li>返回内容不是合法 JSON / 字段缺失 → 切换；</li>
 *   <li>HTTP 401 / 403（Key 错误或无权限）、402（余额不足）、429（请求过快）→ 不切换，直接提示。</li>
 * </ul>
 * 表中未列出的其他 4xx（如 422 参数错误）与 400 同属请求问题，也切换备用模型；
 * 业务错误（未登录、未配置 Key 等）不切换。
 */
public final class VisionErrors {
    private VisionErrors() {
    }

    public static boolean shouldFallback(@Nullable Throwable e) {
        if (e == null) return false;
        int code = httpCode(e);
        if (code > 0) return shouldFallbackForHttp(code);
        if (e instanceof TimeoutException) return true;
        if (e instanceof IOException) return true;
        if (e instanceof JsonParseException) return true;
        return false;
    }

    public static boolean shouldFallbackForHttp(int code) {
        if (code == 401 || code == 402 || code == 403 || code == 429) return false;
        return code >= 400;
    }

    /** HTTP 状态码；不是 HTTP 错误时为 0 */
    static int httpCode(Throwable e) {
        if (e instanceof AppError.HttpCodeException) return ((AppError.HttpCodeException) e).code;
        if (e instanceof HttpException) return ((HttpException) e).code();
        return 0;
    }
}
