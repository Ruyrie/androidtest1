package com.heban.app.data.model;

/** 逐日预报（/v7/weather/7d）。 */
public final class DailyWeather {
    /** 当天 0 点（毫秒） */
    public long date;
    public int tempMax;
    public int tempMin;
    public String textDay;
    public String iconDay;
    /** 夜间天气文字，与白天不同时显示为「多云转晴」 */
    public String textNight;
    public String iconNight;
    public double precip;
    public int humidity;
    public String windScaleDay;

    /** 「多云转晴」：白天与夜间天气不同时用「转」连接 */
    public String summaryText() {
        if (textNight == null || textNight.isEmpty() || textNight.equals(textDay)) return textDay;
        if (textDay == null || textDay.isEmpty()) return textNight;
        return textDay + "转" + textNight;
    }
}
