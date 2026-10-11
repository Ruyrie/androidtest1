package com.heban.app.data.remote.weather;

/** GET /v7/weather/now 返回体（数值字段在和风天气中均为字符串）。 */
public class QwNowResp {
    public String code;
    public String updateTime;
    public Now now;

    public static class Now {
        /** 如 "2020-06-30T21:40+08:00" */
        public String obsTime;
        public String temp;
        public String feelsLike;
        public String icon;
        public String text;
        public String windDir;
        public String windScale;
        public String humidity;
        public String precip;
    }
}
