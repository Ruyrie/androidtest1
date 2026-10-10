package com.heban.app.data.model;

/** 逐日预报（/v7/weather/7d）。 */
public final class DailyWeather {
    /** 当天 0 点（毫秒） */
    public long date;
    public int tempMax;
    public int tempMin;
    public String textDay;
    public String iconDay;
    public double precip;
    public int humidity;
    public String windScaleDay;
}
