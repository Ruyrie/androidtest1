package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 动态与提问（S01–S06）。模拟内容 farmer_id 为作者农友、user_id = 0；用户自己发布的 user_id = 当前账号、farmer_id = 0。
 * 计数（赞、评论、收藏）为显示值，当前账号的点赞 / 收藏状态在 like / favorite 表中。
 */
@Entity(tableName = "post", indices = {@Index("farmer_id"), @Index("user_id"), @Index("created_at")})
public class PostEntity {
    public static final String TYPE_POST = "POST";
    public static final String TYPE_QUESTION = "QUESTION";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_SOLVED = "SOLVED";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String type = TYPE_POST;

    @ColumnInfo(name = "farmer_id")
    public long farmerId;

    @ColumnInfo(name = "user_id")
    public long userId;

    /** 提问标题（动态为空） */
    public String title;

    @NonNull
    public String content = "";

    /** 图片路径，JSON 字符串数组（最多 9 张） */
    public String images;

    /** 话题，如「#草莓种植」 */
    public String topic;

    /** 关联作物名（问答「与我作物相关」筛选用），如「番茄」 */
    @ColumnInfo(name = "crop_name")
    public String cropName;

    /** 关联我的作物 id（自己发布时） */
    @ColumnInfo(name = "crop_id")
    public Long cropId;

    /** 发布位置，如「句容市白兔镇」 */
    public String location;

    /** 同城筛选用的城市，如「南京」 */
    public String city;

    /** 谁可以看：所有人 / 我关注的人 / 仅自己 */
    public String visibility;

    public int likes;

    public int comments;

    public int favorites;

    public int views;

    /** 提问的关注人数 */
    public int followers;

    /** 悬赏积分，0 为无 */
    public int reward;

    /** OPEN / SOLVED（提问） */
    public String status;

    /** 被采纳的回答 id */
    @ColumnInfo(name = "accepted_id")
    public Long acceptedId;

    /** AI 参考答案（提问时开启「让 AI 先给参考答案」） */
    @ColumnInfo(name = "ai_answer")
    public String aiAnswer;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
