package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 地块（C05）。棚内温湿度由用户手动记录。 */
@Entity(tableName = "plot", indices = @Index("user_id"))
public class PlotEntity {
    public static final String TYPE_GREENHOUSE = "大棚";
    public static final String TYPE_OPEN = "露天";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    public String name = "";

    @ColumnInfo(name = "area_mu")
    public double areaMu;

    /** 大棚 / 露天 */
    @NonNull
    public String type = TYPE_GREENHOUSE;

    /** 棚型或地块描述，如「钢架薄膜棚」「沙壤土」 */
    public String structure;

    /** 灌溉方式，如「滴灌」「漫灌」 */
    public String irrigation;

    public String soil;

    public String note;

    /** 最近一次手动记录的棚内温度（℃），未记录为 null */
    @ColumnInfo(name = "indoor_temp")
    public Double indoorTemp;

    /** 最近一次手动记录的棚内湿度（%），未记录为 null；≥ 85 显示「湿度偏高」 */
    @ColumnInfo(name = "indoor_humidity")
    public Integer indoorHumidity;

    @ColumnInfo(name = "reading_at")
    public long readingAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
