package com.heban.app.data.model;

/** 城市搜索结果（和风 GeoAPI /geo/v2/city/lookup）。 */
public final class CityOption {
    public String id;
    public String name;
    /** 上级城市，如「南京」 */
    public String adm2;
    /** 省份，如「江苏」 */
    public String adm1;

    /** 显示名：「南京 江宁」 */
    public String displayName() {
        if (adm2 == null || adm2.equals(name)) return name;
        return adm2 + " " + name;
    }
}
