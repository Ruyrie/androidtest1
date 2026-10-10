package com.heban.app.data.repository;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.DirectMessageEntity;
import com.heban.app.data.local.entity.NotificationEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.model.ConversationItem;
import com.heban.app.data.model.MessageCounts;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** MessageRepository 的本地实现（Room + 自动回复）。M6 数据层实现前为占位。 */
public final class LocalMessageRepository implements MessageRepository {
    private final Context app;
    private final AppDatabase db;
    private final CurrentUser user;
    private final Gson gson;

    public LocalMessageRepository(Context app, AppDatabase db, CurrentUser user, Gson gson) {
        this.app = app;
        this.db = db;
        this.user = user;
        this.gson = gson;
    }

    @Override
    public Flowable<MessageCounts> observeCounts() {
        return Flowable.just(new MessageCounts());
    }

    @Override
    public Flowable<List<ConversationItem>> observeConversations() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<ConversationItem> observeConversation(long conversationId) {
        return Flowable.empty();
    }

    @Override
    public Flowable<List<DirectMessageEntity>> observeMessages(long conversationId) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<Boolean> observeTyping(long conversationId) {
        return Flowable.just(false);
    }

    @Override
    public Single<Long> openConversation(long farmerId, @Nullable Long productId) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Single<Long> openServiceConversation() {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable send(long conversationId, String text) {
        return Completable.complete();
    }

    @Override
    public Single<List<String[]>> serviceQuickQuestions() {
        return Single.just(Collections.emptyList());
    }

    @Override
    public Completable markConversationRead(long conversationId) {
        return Completable.complete();
    }

    @Override
    public Completable markAllRead() {
        return Completable.complete();
    }

    @Override
    public Flowable<List<NotificationEntity>> observeNotifications(String type) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Completable markNotificationsRead(String type) {
        return Completable.complete();
    }

    @Override
    public Completable addNotification(NotificationEntity notification) {
        return Completable.complete();
    }
}
