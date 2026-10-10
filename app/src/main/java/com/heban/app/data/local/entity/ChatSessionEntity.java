package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** AI 助手会话（D06 / M11）。 */
@Entity(tableName = "chat_session", indices = @Index("user_id"))
public class ChatSessionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    /** 会话标题：第一个问题的前 20 个字 */
    @NonNull
    public String title = "";

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
