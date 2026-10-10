package com.heban.app.data.repository;

import com.heban.app.data.model.CityOption;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.WeatherState;

import java.time.LocalDate;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 和风天气（技术准备文档 8.2）：先发射缓存，超过 30 分钟再并发请求实时 / 逐小时 / 7 天并写缓存。
 * 地点：已保存的 weather.location_id；没有时用定位（区县级）或用户资料中的地区通过 GeoAPI 解析。
 * 未配置 QWEATHER_API_HOST / QWEATHER_API_KEY 时只发射带 error 的状态（不显示示例数据）。
 * 每次拿到新天气后，按规则更新作物的「需关注」（天气自动判断）。
 */
public interface WeatherRepository {

    /** 持续发射天气状态（loading → 缓存 → 最新 / error） */
    Flowable<WeatherState> observe();

    /** 手动刷新（下拉刷新）；force 时忽略 30 分钟缓存 */
    Completable refresh(boolean force);

    /** H02 切换地点：城市搜索 */
    Single<List<CityOption>> searchCity(String keyword);

    Completable setLocation(CityOption city);

    /**
     * H04「AI 提示」：根据缓存的逐小时 / 逐日预报给出这类农事在 day 前后的最佳作业时段（FarmIndexCalculator，不调用大模型），
     * 如「11 日 7:00–10:00 风力 1 级、无降水，适合喷药；12 日起有降雨，建议提前完成。」（不含「AI 提示：」前缀）。
     * 没有天气数据或无法判断时返回空字符串。
     */
    Single<String> farmTaskTip(FarmType type, LocalDate day);
}
