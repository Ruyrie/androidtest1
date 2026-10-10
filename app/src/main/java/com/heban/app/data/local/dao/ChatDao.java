package com.heban.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.ChatSessionEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ChatDao {
    @Query("SELECT * FROM chat_session WHERE user_id = :userId ORDER BY updated_at DESC")
    Flowable<List<ChatSessionEntity>> observeSessions(long userId);

    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId ORDER BY created_at, id")
    Flowable<List<ChatMessageEntity>> observeMessages(long sessionId);

    @Insert
    Single<Long> insertSession(ChatSessionEntity session);

    @Update
    Completable updateSession(ChatSessionEntity session);

    @Insert
    Single<Long> insertMessage(ChatMessageEntity message);

    @Update
    Completable updateMessage(ChatMessageEntity message);

    @Query("DELETE FROM chat_session WHERE id = :id")
    Completable deleteSession(long id);
}
