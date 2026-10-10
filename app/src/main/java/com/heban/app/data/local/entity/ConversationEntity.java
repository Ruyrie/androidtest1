package com.heban.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 私信会话（S08 / S09 / P09）。按账号隔离；新账号为空。 */
@Entity(tableName = "conversation", indices = @Index("user_id"))
public class ConversationEntity {
    public static final String KIND_FARMER = "FARMER";
    /** 禾伴客服（P09） */
    public static final String KIND_SERVICE = "SERVICE";
    /** 禾伴小助手（系统消息） */
    public static final String KIND_ASSISTANT = "ASSISTANT";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @NonNull
    public String kind = KIND_FARMER;

    /** 对方农友 id（kind = FARMER） */
    @ColumnInfo(name = "farmer_id")
    public long farmerId;

    /** 关联的集市商品（S09 顶部商品卡片），可为空 */
    @ColumnInfo(name = "product_id")
    public Long productId;

    @ColumnInfo(name = "last_message")
    public String lastMessage;

    @ColumnInfo(name = "last_at")
    public long lastAt;

    public int unread;
}
