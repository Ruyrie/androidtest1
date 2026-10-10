package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 农事记录（C02 / C04）。类型见 {@link com.heban.app.data.model.FarmType}。 */
@Entity(tableName = "farm_record", indices = {@Index("user_id"), @Index("crop_id")})
public class FarmRecordEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "crop_id")
    public long cropId;

    /** FarmType.code：FERTILIZE / SPRAY / IRRIGATE / PRUNE / HARVEST / OTHER */
    @NonNull
    public String type = "OTHER";

    /** 作业日期时间（毫秒） */
    public long date;

    /** 物料或内容：肥料名称 / 药剂名称 / 灌溉方式 / 作业内容 */
    public String material;

    /** 用量或采收量 */
    public double amount;

    public String unit;

    @ColumnInfo(name = "cost_cent")
    public long costCent;

    /** 现场照片路径（App 私有目录），JSON 字符串数组 */
    public String photos;

    /** 同步生成的记账条目 id */
    @ColumnInfo(name = "ledger_entry_id")
    public Long ledgerEntryId;

    public String note;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
