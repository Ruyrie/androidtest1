package com.heban.app.data.model;

import com.heban.app.data.local.entity.CropEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;

import java.util.ArrayList;
import java.util.List;

/** C02 作物详情：作物、所在地块、生长阶段与农事记录（按日期倒序）。 */
public final class CropDetail {
    public CropEntity crop;
    public String plotName;
    public CropCard card;
    public List<StageInfo> stages = new ArrayList<>();
    /** 距预计采收还需多少天，已过期为 0 */
    public int daysLeft;
    /** 预计采收日期（毫秒） */
    public long expectedHarvestAt;
    public List<FarmRecordEntity> records = new ArrayList<>();
}
