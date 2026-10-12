package com.heban.app.data.local.dao;

import androidx.annotation.Nullable;
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

    /** 某账号的会话消息（会话不属于该账号时为空列表） */
    @Query("SELECT m.* FROM chat_message m INNER JOIN chat_session s ON s.id = m.session_id "
            + "WHERE m.session_id = :sessionId AND s.user_id = :userId ORDER BY m.created_at, m.id")
    Flowable<List<ChatMessageEntity>> observeMessagesForUser(long sessionId, long userId);

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

    // —— 同步方法：只在 io 线程 / 事务中调用 ——

    @Nullable
    @Query("SELECT * FROM chat_session WHERE id = :id AND user_id = :userId")
    ChatSessionEntity getSessionSync(long id, long userId);

    @Insert
    long insertSessionSync(ChatSessionEntity session);

    @Insert
    long insertMessageSync(ChatMessageEntity message);

    @Query("UPDATE chat_session SET updated_at = :time WHERE id = :id")
    int touchSessionSync(long id, long time);

    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId ORDER BY created_at, id")
    List<ChatMessageEntity> listMessagesSync(long sessionId);

    /** 某账号会话中的一条消息（不属于该账号时为 null） */
    @Nullable
    @Query("SELECT m.* FROM chat_message m INNER JOIN chat_session s ON s.id = m.session_id "
            + "WHERE m.id = :id AND s.user_id = :userId")
    ChatMessageEntity getMessageSync(long id, long userId);

    @Nullable
    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId ORDER BY created_at DESC, id DESC LIMIT 1")
    ChatMessageEntity lastMessageSync(long sessionId);

    @Query("DELETE FROM chat_message WHERE id = :id")
    int deleteMessageSync(long id);

    @Query("UPDATE chat_message SET feedback = :feedback WHERE id = :id "
            + "AND session_id IN (SELECT id FROM chat_session WHERE user_id = :userId)")
    int setFeedbackSync(long id, long userId, int feedback);

    @Query("SELECT image_path FROM chat_message WHERE session_id = :sessionId AND image_path IS NOT NULL")
    List<String> imagePathsInSessionSync(long sessionId);

    @Query("SELECT m.image_path FROM chat_message m INNER JOIN chat_session s ON s.id = m.session_id "
            + "WHERE s.user_id = :userId AND m.image_path IS NOT NULL")
    List<String> imagePathsForUserSync(long userId);

    @Query("DELETE FROM chat_message WHERE session_id = :sessionId")
    int deleteMessagesInSessionSync(long sessionId);

    @Query("DELETE FROM chat_session WHERE id = :id AND user_id = :userId")
    int deleteSessionSync(long id, long userId);

    @Query("DELETE FROM chat_message WHERE session_id IN (SELECT id FROM chat_session WHERE user_id = :userId)")
    int deleteMessagesForUserSync(long userId);

    @Query("DELETE FROM chat_session WHERE user_id = :userId")
    int deleteSessionsForUserSync(long userId);

    @Query("SELECT COUNT(*) FROM chat_session WHERE user_id = :userId")
    int countSessionsSync(long userId);

    /** 某账号全部消息文本的字节数（UTF-8） */
    @Query("SELECT COALESCE(SUM(LENGTH(CAST(m.content AS BLOB))), 0) FROM chat_message m "
            + "INNER JOIN chat_session s ON s.id = m.session_id WHERE s.user_id = :userId")
    long textBytesSync(long userId);
}
