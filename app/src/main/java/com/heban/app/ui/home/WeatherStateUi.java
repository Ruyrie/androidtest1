package com.heban.app.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.model.WeatherAlert;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.model.WeatherState;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.weather.QWeatherErrors;

/**
 * H01 天气卡 / H02 天气详情共用的显示状态（UI 设计文档表 7-2）：
 * 有数据显示数据（离线时顶部加离线提示栏）；没有数据且没有错误显示骨架屏；没有数据但有错误时在卡片位置报错：
 * 缺少地点时提供「选择城市」，网络类错误提供「重试」，缺少 Key 等配置问题只显示提示。
 */
public final class WeatherStateUi {
    @NonNull public final WeatherState state;

    public WeatherStateUi(@Nullable WeatherState state) {
        this.state = state != null ? state : loading();
    }

    public static WeatherState loading() {
        WeatherState s = new WeatherState();
        s.loading = true;
        return s;
    }

    /** observe() 本身出错时转换成带 error 的状态 */
    public static WeatherState failed(Throwable t) {
        WeatherState s = new WeatherState();
        s.error = AppError.from(t);
        return s;
    }

    @Nullable
    public WeatherBundle bundle() {
        return state.bundle != null && state.bundle.now != null ? state.bundle : null;
    }

    public boolean hasData() {
        return bundle() != null;
    }

    public boolean hasIndices() {
        return hasData() && state.indices != null && !state.indices.isEmpty();
    }

    public boolean hasDaily() {
        WeatherBundle b = bundle();
        return b != null && b.daily != null && !b.daily.isEmpty();
    }

    public boolean showSkeleton() {
        return !hasData() && state.error == null;
    }

    public boolean showError() {
        return !hasData() && state.error != null;
    }

    public boolean showOffline() {
        return hasData() && state.offline;
    }

    public boolean showAlert() {
        return state.alert != null;
    }

    @Nullable
    public WeatherAlert alert() {
        return state.alert;
    }

    @NonNull
    public String errorMessage() {
        return state.error == null ? "" : state.error.message;
    }

    /** 缺少地点（定位被拒绝且资料中没有所在地区）：提供「选择城市」 */
    public boolean needsCityPick() {
        return showError() && isLocationError(state.error);
    }

    /** 网络、超时、服务端等可重试的错误：提供「重试」 */
    public boolean canRetry() {
        if (!showError() || needsCityPick()) return false;
        AppError.Type t = state.error.type;
        return t != AppError.Type.BUSINESS && t != AppError.Type.UNAUTHORIZED;
    }

    @Nullable
    public String locationName() {
        WeatherBundle b = bundle();
        return b == null || b.locationName == null || b.locationName.isEmpty() ? null : b.locationName;
    }

    @Nullable
    public String farmTip() {
        return hasData() && state.farmTip != null && !state.farmTip.isEmpty() ? state.farmTip : null;
    }

    static boolean isLocationError(@Nullable AppError e) {
        return QWeatherErrors.isNoLocation(e);
    }
}
