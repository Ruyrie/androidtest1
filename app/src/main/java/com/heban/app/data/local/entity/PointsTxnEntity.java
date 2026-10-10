package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 积分流水（P01、H05、M01）。余额 = SUM(amount)，成长值 = SUM(growth)；只追加，不修改。 */
@Entity(tableName = "points_txn", indices = {@Index("user_id"), @Index("created_at")})
public class PointsTxnEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    /** PointsRules.TYPE_*，如 CHECKIN、FARM_TASK、LEDGER、PIN、REDEEM */
    @NonNull
    public String type = "";

    /** 明细标题，如「每日签到 · 连续第 5 天」「集市置顶「樱桃番茄」24 小时」 */
    @NonNull
    public String title = "";

    /** 积分，可为负 */
    public int amount;

    /** 成长值（≥ 0） */
    public int growth;

    /** 关联对象 id（商品、订单、问题等），可为空 */
    @ColumnInfo(name = "ref_id")
    public String refId;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
