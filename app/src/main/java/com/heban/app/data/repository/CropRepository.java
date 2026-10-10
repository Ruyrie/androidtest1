package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.PlotEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.CropDetail;
import com.heban.app.data.model.CropProfile;
import com.heban.app.data.model.CropStats;
import com.heban.app.data.model.PlotCard;
import com.heban.app.data.model.PlotStats;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 作物、地块、农事记录（两个 flavor 共用的本地实现，技术准备文档表 5-2）。
 * 数据按当前登录账号隔离（user_id）；未登录 / 游客时查询返回空列表，写操作抛出 BusinessException("NOT_LOGGED_IN")。
 * 所有方法内部切到 io 线程。
 */
public interface CropRepository {
    String FILTER_ALL = "全部";
    String FILTER_GREENHOUSE = "大棚";
    String FILTER_OPEN = "露天";
    String FILTER_ATTENTION = "需关注";

    // —— 作物 ——
    /** C01 / H01 作物卡片；filter 取 FILTER_*（null 视为全部），按定植日期倒序 */
    Flowable<List<CropCard>> observeCropCards(@Nullable String filter);

    Flowable<CropStats> observeCropStats();

    /** C02 作物详情（作物不存在时完成而不发射） */
    Flowable<CropDetail> observeCropDetail(long cropId);

    Single<CropEntity> getCrop(long cropId);

    /** 新增（id == 0）或更新作物，返回作物 id；未填 expectedDays 时取生长期表默认值 */
    Single<Long> saveCrop(CropEntity crop);

    /** 删除作物及其农事记录，并取消相关提醒 */
    Completable deleteCrop(long cropId);

    /** 手动取消「需关注」 */
    Completable dismissAttention(long cropId);

    /** 标记「需关注」（识别结果保存到作物时使用，source = CropEntity.ATTENTION_SCAN） */
    Completable markAttention(long cropId, String note, String source);

    /** 当前账号的全部作物（天气规则判断用） */
    Single<List<CropEntity>> listCrops();

    /**
     * 天气自动判断的结果：note 不为空时标记（source = weather），但用户手动取消后 3 天内不标记、
     * 不覆盖识别结果（scan）的标记；note 为空时清除来源为 weather 的标记。
     */
    Completable setWeatherAttention(long cropId, @Nullable String note);

    /** 作物生长期表（assets/mock/crop_profiles.json），找不到时返回通用默认值 */
    CropProfile profileFor(String cropName);

    List<CropProfile> allProfiles();

    // —— 地块 ——
    Flowable<List<PlotEntity>> observePlots();

    Flowable<List<PlotCard>> observePlotCards();

    Flowable<PlotStats> observePlotStats();

    Single<Long> savePlot(PlotEntity plot);

    /** 删除地块；地块中的作物 plot_id 置空 */
    Completable deletePlot(long plotId);

    /** 手动记录棚内温湿度 */
    Completable recordPlotReading(long plotId, double tempCelsius, int humidityPercent);

    // —— 农事记录 ——
    /** C04 保存农事记录；syncToLedger 且 costCent > 0 时在同一事务内写一笔支出（技术准备文档 7.2） */
    Single<Long> saveRecord(FarmRecordEntity record, boolean syncToLedger);

    Completable deleteRecord(long recordId);
}
