package com.heban.app.data.remote.weather;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * 和风天气接口（技术准备文档表 8-2）。Base URL 为每个开发者独立的 API Host，
 * 认证请求头 X-QW-Api-Key 由 {@link QWeatherKeyInterceptor} 添加。
 * 成功时返回体中 code 为 "200"，其余情况见 {@link QWeatherErrors}。
 */
public interface QWeatherApi {

    /**
     * 城市搜索：location 为名称（如「江宁」）、LocationID 或「经度,纬度」（十进制，最多两位小数）。
     *
     * @param adm    上级行政区划，用于排除重名城市，可为 null
     * @param range  搜索范围，如 "cn"，可为 null
     * @param number 返回数量 1–20，可为 null（默认 10）
     */
    @GET("geo/v2/city/lookup")
    Single<QwCityResp> lookupCity(@Query("location") String keywordOrLonLat,
                                  @Query("adm") String adm,
                                  @Query("range") String range,
                                  @Query("number") Integer number);

    @GET("v7/weather/now")
    Single<QwNowResp> now(@Query("location") String locationId);

    @GET("v7/weather/24h")
    Single<QwHourlyResp> hourly(@Query("location") String locationId);

    @GET("v7/weather/7d")
    Single<QwDailyResp> daily(@Query("location") String locationId);
}
