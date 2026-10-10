package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.PestEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/** 病虫害百科（D01 / D04 / D05），数据全部在本地，离线可用。 */
public interface PestRepository {
    /** D04 列表：keyword 匹配名称 / 别名 / 作物，kind 为 病害 / 虫害 / 生理性（null 为全部），crop 为作物（null 为全部） */
    Flowable<List<PestEntity>> search(@Nullable String keyword, @Nullable String kind, @Nullable String crop);

    /** 本季高发：months 包含当前月份的条目（按 season_level 高发在前） */
    Flowable<List<PestEntity>> seasonal(int month);

    Flowable<Integer> count();

    Single<PestEntity> get(long id);

    /** 按名称或别名匹配（识别结果跳百科用），找不到时完成而不发射 */
    Maybe<PestEntity> matchByName(String name);

    /** D04「作物」筛选的选项 */
    Single<List<String>> cropOptions();
}
