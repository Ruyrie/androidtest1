package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 积分商城商品（P03 / P04），来自 assets/mock/mall_items.json，所有账号共用。 */
@Entity(tableName = "mall_item")
public class MallItemEntity {
    public static final String TYPE_REAL = "实物";
    public static final String TYPE_DECO = "装扮";

    @PrimaryKey
    public long id;

    @NonNull
    public String name = "";

    /** 实物 / 装扮 */
    @NonNull
    public String type = TYPE_REAL;

    /** 所需积分 */
    public int price;

    public int stock;

    /** 已兑换件数（显示值） */
    public int sold;

    @ColumnInfo(name = "limit_per_user")
    public int limitPerUser;

    /** Material Symbol 名称（无图片时的占位图标），如 nutrition / back_hand */
    public String icon;

    /** 图片路径，JSON 字符串数组 */
    public String images;

    /** 商品说明 */
    public String description;

    /** 发货说明，如「包邮 · 3 天内发货」 */
    @ColumnInfo(name = "ship_note")
    public String shipNote;
}
