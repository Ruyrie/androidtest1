package com.heban.app.data.remote.weather;

import java.util.List;

/** GET /geo/v2/city/lookup 返回体。 */
public class QwCityResp {
    public String code;
    public List<Location> location;

    public static class Location {
        public String name;
        public String id;
        public String lat;
        public String lon;
        /** 上级行政区划，如「南京」 */
        public String adm2;
        /** 一级行政区域，如「江苏省」 */
        public String adm1;
        public String country;
        public String type;
    }
}
