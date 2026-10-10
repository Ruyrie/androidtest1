package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 病虫害百科（D04 / D05），来自 assets/mock/pests.json，所有账号共用。 */
@Entity(tableName = "pest")
public class PestEntity {
    public static final String KIND_DISEASE = "病害";
    public static final String KIND_INSECT = "虫害";
    public static final String KIND_PHYSIOLOGICAL = "生理性";

    @PrimaryKey
    public long id;

    /** 如「黄瓜霜霉病」 */
    @NonNull
    public String name = "";

    /** 别名，顿号分隔，如「跑马干、黑毛」 */
    public String aliases;

    /** 英文名，如「Cucumber downy mildew」 */
    @ColumnInfo(name = "en_name")
    public String enName;

    /** D04 筛选：病害 / 虫害 / 生理性 */
    @NonNull
    public String kind = KIND_DISEASE;

    /** 细分类别，如「卵菌病害」「真菌病害」「虫害」「生理性病害」 */
    public String category;

    /** 危害作物（显示用），如「黄瓜、甜瓜等葫芦科作物」 */
    public String crops;

    /** D04 作物筛选 / 列表副标题用的作物短名，顿号分隔，如「黄瓜、白菜」 */
    @ColumnInfo(name = "crop_tags")
    public String cropTags;

    /** 发病部位，如「叶部」 */
    public String part;

    /** 高发季节文字，如「春秋高发」 */
    @ColumnInfo(name = "season_text")
    public String seasonText;

    /** 高发月份，逗号分隔，如「3,4,5,9,10,11」（D01 / D04「本季高发」） */
    public String months;

    /** 本季高发时的程度：高发 / 较高 */
    @ColumnInfo(name = "season_level")
    public String seasonLevel;

    /** 病原，如「古巴假霜霉菌」 */
    public String pathogen;

    /** 易发条件 */
    public String conditions;

    /** 症状识别 */
    public String symptoms;

    /** 一句话提示，如「秋季低温高湿易发」 */
    public String tip;

    /** 农业防治措施，JSON 字符串数组 */
    @ColumnInfo(name = "prevention_agri")
    public String preventionAgri;

    /** 化学防治措施（只写通用名，并提醒按标签用量），JSON 字符串数组 */
    @ColumnInfo(name = "prevention_chem")
    public String preventionChem;

    /** 图片相对路径 images/pests/…，JSON 字符串数组；第一张为封面 */
    public String images;

    /** 图片说明，JSON 字符串数组，与 images 对应，如「叶片正面症状」 */
    @ColumnInfo(name = "image_captions")
    public String imageCaptions;

    /** Material Symbol 名称，如 coronavirus / blur_on / bug_report */
    public String icon;
}
