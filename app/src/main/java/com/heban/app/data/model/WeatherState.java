package com.heban.app.data.model;

import androidx.annotation.Nullable;

import com.heban.app.data.remote.AppError;

import java.util.ArrayList;
import java.util.List;

/**
 * 天气页面状态（H01 / H02）。bundle 为 null 且 error 不为 null 表示无缓存的错误；
 * 二者都有表示显示缓存并提示 error（如离线）。缺少 Key 时只给出错误提示（不显示示例数据）。
 */
public final class WeatherState {
    public boolean loading;
    @Nullable public WeatherBundle bundle;
    public List<FarmIndex> indices = new ArrayList<>();
    @Nullable public WeatherAlert alert;
    /** H01 天气卡底部的农事建议，如「15:00–18:00 风力小，适宜喷药」 */
    @Nullable public String farmTip;
    /** 当前显示的是缓存（请求失败或离线） */
    public boolean offline;
    @Nullable public AppError error;
}
