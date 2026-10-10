package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 集市商品（D01 / D07–D09）。模拟商品来自 assets/mock/products.json（所有账号共用，seller_id 为农友 id）；
 * 用户自己发布的商品 user_id = 当前账号、mine = true。卖家信息冗余保存，便于离线展示。
 */
@Entity(tableName = "product", indices = {@Index("category"), @Index("user_id")})
public class ProductEntity {
    public static final String CAT_PRODUCE = "农产品";
    public static final String CAT_SUPPLY = "农资";
    public static final String CAT_SEEDLING = "种苗";
    public static final String CAT_MACHINE = "农机具";

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 发布者账号（自己发布时为当前账号 id，模拟商品为 0） */
    @ColumnInfo(name = "user_id")
    public long userId;

    public boolean mine;

    /** 卖家农友 id（farmers.json），自己发布时为 0 */
    @ColumnInfo(name = "seller_id")
    public long sellerId;

    @ColumnInfo(name = "seller_name")
    public String sellerName;

    /** 卖家所在地，如「句容白兔镇」 */
    @ColumnInfo(name = "seller_region")
    public String sellerRegion;

    @ColumnInfo(name = "seller_years")
    public int sellerYears;

    /** 好评率 0–100 */
    @ColumnInfo(name = "seller_rating")
    public int sellerRating;

    @ColumnInfo(name = "seller_verified")
    public boolean sellerVerified;

    @ColumnInfo(name = "seller_phone")
    public String sellerPhone;

    @NonNull
    public String title = "";

    /** 农产品 / 农资 / 种苗 / 农机具 */
    @NonNull
    public String category = CAT_PRODUCE;

    /** 细分，如「水果」（D09「农产品 · 水果」） */
    @ColumnInfo(name = "sub_category")
    public String subCategory;

    @ColumnInfo(name = "price_cent")
    public long priceCent;

    /** 单位：斤 / 袋 / 株 / 台 …，可为空（整件） */
    public String unit;

    /** 起售量，如 10（斤） */
    @ColumnInfo(name = "min_qty")
    public double minQty;

    /** 交易方式，顿号分隔：自提、同城配送、快递、面议 */
    @ColumnInfo(name = "trade_ways")
    public String tradeWays;

    /** 交易地点，如「江苏省 句容市 白兔镇」 */
    public String location;

    /** 图片相对路径或 App 私有目录路径，JSON 字符串数组；第一张为封面 */
    public String images;

    /** 列表角标，如「产地直供」「二手」「可配送」「种苗」 */
    public String tag;

    /** 详情页特征标签，JSON 字符串数组，如 ["产地直供","可自提","同城配送","附种植记录"] */
    public String tags;

    public String description;

    /** 规格，如「2 斤 / 盒，单果 20–30 g」 */
    public String spec;

    /** 采摘 / 库存说明，如「每日现摘，库存约 300 斤」 */
    @ColumnInfo(name = "stock_note")
    public String stockNote;

    /** 距离（公里），自己发布的为 0 */
    @ColumnInfo(name = "distance_km")
    public double distanceKm;

    public int views;

    /** 想要人数 */
    public int wants;

    /** 置顶截止时间（毫秒），未置顶为 0 */
    @ColumnInfo(name = "pinned_until")
    public long pinnedUntil;

    /** 「附上种植记录」关联的作物 */
    @ColumnInfo(name = "crop_id")
    public Long cropId;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
