package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 动态评论 / 问题回答（S02 / S05）。作者为农友（farmer_id）或当前账号（user_id）。 */
@Entity(tableName = "comment", indices = {@Index("post_id"), @Index("user_id")})
public class CommentEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "post_id")
    public long postId;

    @ColumnInfo(name = "farmer_id")
    public long farmerId;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    public String content = "";

    /** 动态作者的回复（S02「草莓姐姐 作者：…」），可为空 */
    @ColumnInfo(name = "author_reply")
    public String authorReply;

    /** 点赞 / 赞同数（显示值） */
    public int likes;

    /** 已采纳（回答） */
    public boolean accepted;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
