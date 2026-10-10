package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.ConversationEntity;
import com.heban.app.data.local.entity.DirectMessageEntity;
import com.heban.app.data.local.entity.NotificationEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** 私信与消息通知。 */
@Dao
public interface MessageDao {
    @Query("SELECT * FROM conversation WHERE user_id = :userId ORDER BY last_at DESC")
    Flowable<List<ConversationEntity>> observeConversations(long userId);

    @Query("SELECT * FROM direct_message WHERE conversation_id = :conversationId ORDER BY created_at, id")
    Flowable<List<DirectMessageEntity>> observeMessages(long conversationId);

    @Query("SELECT * FROM notification WHERE user_id = :userId AND type = :type ORDER BY created_at DESC")
    Flowable<List<NotificationEntity>> observeNotifications(long userId, String type);

    @Insert
    Single<Long> insertConversation(ConversationEntity conversation);

    @Update
    Completable updateConversation(ConversationEntity conversation);

    @Insert
    Single<Long> insertMessage(DirectMessageEntity message);

    @Insert
    Single<Long> insertNotification(NotificationEntity notification);
}
