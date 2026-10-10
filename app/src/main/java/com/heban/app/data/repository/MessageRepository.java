package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.DirectMessageEntity;
import com.heban.app.data.local.entity.NotificationEntity;
import com.heban.app.data.model.ConversationItem;
import com.heban.app.data.model.MessageCounts;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 私信与消息通知（S08 / S09 / P09）。按当前账号保存；新账号为空。
 * S09 对方回复由 MockReplyEngine 按 replies.json 关键词选择，1.5 秒后插入；P09 客服按 service_faq.json 1 秒后回复。
 */
public interface MessageRepository {
    Flowable<MessageCounts> observeCounts();

    Flowable<List<ConversationItem>> observeConversations();

    Flowable<ConversationItem> observeConversation(long conversationId);

    Flowable<List<DirectMessageEntity>> observeMessages(long conversationId);

    /** 对方是否正在输入（S09「对方正在输入…」、P09「客服正在输入…」） */
    Flowable<Boolean> observeTyping(long conversationId);

    /** 打开与农友的会话（不存在则创建），productId 为集市商品（S09 顶部卡片）；返回会话 id */
    Single<Long> openConversation(long farmerId, @Nullable Long productId);

    /** 打开禾伴客服会话（P09） */
    Single<Long> openServiceConversation();

    Completable send(long conversationId, String text);

    /** P09 快捷问题：[问题, 回答] 列表 */
    Single<List<String[]>> serviceQuickQuestions();

    Completable markConversationRead(long conversationId);

    /** S08「全部已读」 */
    Completable markAllRead();

    Flowable<List<NotificationEntity>> observeNotifications(String type);

    Completable markNotificationsRead(String type);

    /** 其他模块写入一条通知（如采纳、物流） */
    Completable addNotification(NotificationEntity notification);
}
