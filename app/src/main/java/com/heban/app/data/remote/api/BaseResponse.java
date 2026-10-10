package com.heban.app.data.remote.api;

import com.google.gson.JsonParseException;
import com.heban.app.data.remote.BusinessException;

/**
 * 禾伴后端统一返回体（技术准备文档 8.4）：{"code": 0, "message": "ok", "data": {...}}，code 不为 0 表示业务错误。
 */
public class BaseResponse<T> {
    public static final int CODE_OK = 0;
    private static final String FALLBACK_MESSAGE = "操作失败，请重试";

    public int code;
    public String message;
    public T data;

    public boolean isOk() {
        return code == CODE_OK;
    }

    /** code 不为 0 时抛出 BusinessException(code, message) */
    public void check() {
        if (!isOk()) {
            boolean noMessage = message == null || message.trim().isEmpty();
            throw new BusinessException(String.valueOf(code), noMessage ? FALLBACK_MESSAGE : message);
        }
    }

    /** 检查 code 并返回 data；data 为空时按「返回内容无法识别」处理 */
    public T dataOrThrow() {
        check();
        if (data == null) {
            throw new JsonParseException("data 为空");
        }
        return data;
    }
}
