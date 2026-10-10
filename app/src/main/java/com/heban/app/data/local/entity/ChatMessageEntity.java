package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** AI 助手消息（D06）。 */
@Entity(tableName = "chat_message", indices = @Index("session_id"))
public class ChatMessageEntity {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public static final String STATUS_OK = "ok";
    /** 流式输出中断（失败或「停止生成」），界面标记「回答未完成」，可「重新生成」 */
    public static final String STATUS_INCOMPLETE = "incomplete";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "session_id")
    public long sessionId;

    /** user / assistant */
    @NonNull
    public String role = ROLE_USER;

    @NonNull
    public String content = "";

    /** 用户附带的图片（App 私有目录路径），可为空 */
    @ColumnInfo(name = "image_path")
    public String imagePath;

    @NonNull
    public String status = STATUS_OK;

    /** 1 = 点赞，0 = 无 */
    public int feedback;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
