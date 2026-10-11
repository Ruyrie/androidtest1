package com.heban.app.data.remote.weather;

import androidx.annotation.NonNull;

import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.BusinessException;

/**
 * 和风天气的业务错误（未配置、缺少地点、认证失败、额度不足等），message 为直接显示给用户的中文提示。
 * 继承 BusinessException，所以 {@code AppError.from(e).message} 也能拿到同样的提示；
 * {@link #toAppError()} 额外保留错误类型与 HTTP 状态码。
 */
public class QWeatherException extends BusinessException {
    @NonNull public final AppError.Type type;
    public final int httpCode;

    public QWeatherException(String code, String message, @NonNull AppError.Type type, int httpCode) {
        super(code, message);
        this.type = type;
        this.httpCode = httpCode;
    }

    @NonNull
    public AppError toAppError() {
        String m = getMessage();
        return new AppError(type, httpCode, m == null ? QWeatherErrors.MSG_UNKNOWN : m);
    }
}
