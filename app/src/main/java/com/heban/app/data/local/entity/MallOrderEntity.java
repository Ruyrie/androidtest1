package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 积分兑换订单（P05、P07、P08）。物流节点按创建时间偏移本地生成：
 * 待发货 → 已揽收 → 运输中 → 派送中 → 已签收；装扮类兑换后直接为「已发放」。
 */
@Entity(tableName = "mall_order", indices = @Index("user_id"))
public class MallOrderEntity {
    public static final String STATUS_WAIT = "待发货";
    public static final String STATUS_PICKED = "已揽收";
    public static final String STATUS_SHIPPING = "运输中";
    public static final String STATUS_DELIVERING = "派送中";
    public static final String STATUS_SIGNED = "已签收";
    public static final String STATUS_ISSUED = "已发放";

    /** 订单号，如 DH202610100917 */
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "order_no")
    public String orderNo = "";

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "item_id")
    public long itemId;

    @ColumnInfo(name = "item_name")
    public String itemName;

    @ColumnInfo(name = "item_type")
    public String itemType;

    @ColumnInfo(name = "item_icon")
    public String itemIcon;

    public int qty;

    /** 共消耗积分 */
    public int points;

    /** 收货地址快照（AddressEntity 的 JSON） */
    @ColumnInfo(name = "address_json")
    public String addressJson;

    /** 最近一次计算的状态（LogisticsWorker 更新，用于发现变化发通知） */
    @NonNull
    public String status = STATUS_WAIT;

    @ColumnInfo(name = "tracking_no")
    public String trackingNo;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
