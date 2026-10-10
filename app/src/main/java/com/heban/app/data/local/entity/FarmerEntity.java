package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 农友（S01–S07、S09），来自 assets/mock/farmers.json，所有账号共用。 */
@Entity(tableName = "farmer")
public class FarmerEntity {
    @PrimaryKey
    public long id;

    @NonNull
    public String name = "";

    /** 头像文字（昵称首字），如「莓」 */
    @ColumnInfo(name = "avatar_text")
    public String avatarText;

    /** 头像图片（可为空，空时显示文字头像） */
    @ColumnInfo(name = "avatar_path")
    public String avatarPath;

    /** 身份徽标：种植能手 / 农技专家 / 官方 / 机构，为空时显示「Lv.N」 */
    public String badge;

    public int level;

    /** 认证（verified 图标） */
    public boolean verified;

    /** 短地区，如「句容」（列表 meta、同城筛选） */
    public String city;

    /** 完整地区，如「江苏 句容」 */
    public String region;

    /** 列表副标题，如「句容 · 种草莓 8 年」「区农技站 · 蔬菜病虫害」 */
    public String intro;

    public String bio;

    public int years;

    @ColumnInfo(name = "joined_days")
    public int joinedDays;

    @ColumnInfo(name = "follows_count")
    public int followsCount;

    @ColumnInfo(name = "fans_count")
    public int fansCount;

    @ColumnInfo(name = "likes_count")
    public int likesCount;

    /** 回答被采纳次数 */
    @ColumnInfo(name = "accepted_count")
    public int acceptedCount;

    /** 在种作物，JSON 数组 [{"name":"草莓","area":3,"icon":"psychiatry"}] */
    @ColumnInfo(name = "crops_json")
    public String cropsJson;

    /** 私信回复说明，如「通常 1 小时内回复」 */
    @ColumnInfo(name = "reply_note")
    public String replyNote;

    /** 联系电话（集市「联系卖家」） */
    public String phone;
}
