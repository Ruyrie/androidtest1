package com.heban.app.data.remote.weather;

import java.util.List;

/** GET /v7/weather/24h 返回体。 */
public class QwHourlyResp {
    public String code;
    public String updateTime;
    public List<Hour> hourly;

    public static class Hour {
        /** 如 "2021-02-16T15:00+08:00" */
        public String fxTime;
        public String temp;
        public String icon;
        public String text;
        public String windDir;
        public String windScale;
        public String humidity;
        /** 降水概率，可能为空 */
        public String pop;
        public String precip;
    }
}
