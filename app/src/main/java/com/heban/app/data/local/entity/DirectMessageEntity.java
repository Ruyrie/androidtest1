package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 私信消息（S09 / P09）。 */
@Entity(tableName = "direct_message", indices = @Index("conversation_id"))
public class DirectMessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "conversation_id")
    public long conversationId;

    @ColumnInfo(name = "from_me")
    public boolean fromMe;

    @NonNull
    public String content = "";

    @ColumnInfo(name = "image_path")
    public String imagePath;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
