package com.heban.app.data.model;

import java.util.ArrayList;
import java.util.List;

/** 一个地点的完整天气数据（实时 + 逐小时 + 7 天）。 */
public final class WeatherBundle {
    public String locationId;
    /** 显示名，如「南京 江宁」 */
    public String locationName;
    public WeatherNow now;
    public List<HourlyWeather> hourly = new ArrayList<>();
    public List<DailyWeather> daily = new ArrayList<>();
    public long fetchedAt;
}
