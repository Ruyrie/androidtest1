package com.heban.app.data.model;

/** 实时天气（和风 /v7/weather/now）。 */
public final class WeatherNow {
    public int temp;
    public int feelsLike;
    /** 天气文字，如「多云」 */
    public String text;
    /** 和风天气图标代码，如 "101"；用 WeatherIcons 转为 Material Symbol */
    public String icon;
    public String windDir;
    /** 风力等级文字，如 "2" 或 "1-2" */
    public String windScale;
    public int humidity;
    public double precip;
    /** 数据观测时间（毫秒） */
    public long obsTime;
}
