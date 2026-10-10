package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

/** 发布草稿（S03）：动态 / 提问两种模式各一份。 */
@Entity(tableName = "draft", primaryKeys = {"user_id", "type"})
public class DraftEntity {
    @ColumnInfo(name = "user_id")
    public long userId;

    /** PostEntity.TYPE_POST / TYPE_QUESTION */
    @NonNull
    public String type = PostEntity.TYPE_POST;

    public String title;

    public String content;

    public String images;

    public String topic;

    @ColumnInfo(name = "crop_id")
    public Long cropId;

    public String location;

    public String visibility;

    public int reward;

    @ColumnInfo(name = "ai_answer")
    public boolean aiAnswer;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
