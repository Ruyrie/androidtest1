package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 在种作物（C01–C03）。生长阶段按定植天数和作物生长期表计算，不单独存储。 */
@Entity(tableName = "crop", indices = {@Index("user_id"), @Index("plot_id")})
public class CropEntity {
    public static final String MODE_GREENHOUSE = "大棚";
    public static final String MODE_OPEN = "露天";
    public static final String MODE_POT = "盆栽";

    public static final String ATTENTION_SCAN = "scan";
    public static final String ATTENTION_WEATHER = "weather";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    /** 所属地块，可为空 */
    @ColumnInfo(name = "plot_id")
    public Long plotId;

    /** 作物名称，如「番茄」 */
    @NonNull
    public String name = "";

    /** 品种，如「樱桃番茄」 */
    public String variety;

    /** 种植方式：大棚 / 露天 / 盆栽 */
    @NonNull
    @ColumnInfo(name = "plant_mode")
    public String plantMode = MODE_GREENHOUSE;

    @ColumnInfo(name = "area_mu")
    public double areaMu;

    /** 定植日期（毫秒，当天 0 点） */
    @ColumnInfo(name = "planted_at")
    public long plantedAt;

    /** 预计生长期（天），默认取作物生长期表 */
    @ColumnInfo(name = "expected_days")
    public int expectedDays;

    @ColumnInfo(name = "cover_path")
    public String coverPath;

    /** 开启后按生长阶段提醒施肥、用药 */
    @ColumnInfo(name = "remind_enabled")
    public boolean remindEnabled;

    public String note;

    /** 「需关注」说明，如「霜霉病风险」；null 表示无需关注 */
    @ColumnInfo(name = "attention_note")
    public String attentionNote;

    /** 需关注来源：scan（识别结果）/ weather（天气自动判断） */
    @ColumnInfo(name = "attention_source")
    public String attentionSource;

    /** 用户手动取消「需关注」的时间；天气规则在之后 3 天内不再自动标记 */
    @ColumnInfo(name = "attention_dismissed_at")
    public long attentionDismissedAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
