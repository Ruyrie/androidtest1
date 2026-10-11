package com.heban.app.data.remote.weather;

import java.util.List;

/** GET /v7/weather/7d 返回体。 */
public class QwDailyResp {
    public String code;
    public String updateTime;
    public List<Day> daily;

    public static class Day {
        /** 如 "2021-11-15" */
        public String fxDate;
        public String tempMax;
        public String tempMin;
        public String iconDay;
        public String textDay;
        public String iconNight;
        public String textNight;
        public String windDirDay;
        public String windScaleDay;
        public String humidity;
        public String precip;
    }
}
