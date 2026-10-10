package com.heban.app.ui.common;

import androidx.annotation.Nullable;

import com.heban.app.data.remote.AppError;

/** 页面的「加载中 / 成功 / 空 / 错误」四种状态（技术准备文档 5.4）。 */
public final class UiState<T> {
    public enum Status { LOADING, SUCCESS, EMPTY, ERROR }

    public final Status status;
    @Nullable public final T data;
    @Nullable public final AppError error;

    private UiState(Status s, @Nullable T d, @Nullable AppError e) {
        status = s;
        data = d;
        error = e;
    }

    public static <T> UiState<T> loading() { return new UiState<>(Status.LOADING, null, null); }
    public static <T> UiState<T> success(T d) { return new UiState<>(Status.SUCCESS, d, null); }
    public static <T> UiState<T> empty() { return new UiState<>(Status.EMPTY, null, null); }
    public static <T> UiState<T> error(Throwable t) {
        return new UiState<>(Status.ERROR, null, AppError.from(t));
    }

    public boolean isLoading() { return status == Status.LOADING; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isEmpty() { return status == Status.EMPTY; }
    public boolean isError() { return status == Status.ERROR; }
}
