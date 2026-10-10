package com.heban.app.data.remote;

/** 业务错误：短信 Code 不为 OK、验证码错误、账号已存在等，message 直接显示给用户。 */
public class BusinessException extends RuntimeException {
    public final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
