package com.heban.app.data.model;

/** 逐小时预报（/v7/weather/24h）。 */
public final class HourlyWeather {
    public long time;
    public int temp;
    public String text;
    public String icon;
    /** 降水概率 0–100，可能缺失为 -1 */
    public int pop = -1;
    public String windScale;
    public int humidity;
    public double precip;
}
