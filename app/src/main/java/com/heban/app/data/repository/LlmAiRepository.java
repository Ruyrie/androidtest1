package com.heban.app.data.repository;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.local.db.AppDatabase;
import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.ChatSessionEntity;
import com.heban.app.data.local.prefs.CurrentUser;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SettingsStore;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.OkHttpClient;

/** AiRepository 的大模型实现（DeepSeek 等 OpenAI 兼容服务，SSE 流式）。M4 数据层实现前为占位。 */
public final class LlmAiRepository implements AiRepository {
    private final Context app;
    private final OkHttpClient baseClient;
    private final Gson gson;
    private final AppDatabase db;
    private final SettingsStore settings;
    private final SecureStore secureStore;
    private final CurrentUser user;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;
    private final WeatherRepository weatherRepository;

    public LlmAiRepository(Context app, OkHttpClient baseClient, Gson gson, AppDatabase db,
                           SettingsStore settings, SecureStore secureStore, CurrentUser user,
                           UserRepository userRepository, CropRepository cropRepository,
                           WeatherRepository weatherRepository) {
        this.app = app;
        this.baseClient = baseClient;
        this.gson = gson;
        this.db = db;
        this.settings = settings;
        this.secureStore = secureStore;
        this.user = user;
        this.userRepository = userRepository;
        this.cropRepository = cropRepository;
        this.weatherRepository = weatherRepository;
    }

    @Override
    public Flowable<List<ChatSessionEntity>> observeSessions() {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Flowable<List<ChatMessageEntity>> observeMessages(long sessionId) {
        return Flowable.just(Collections.emptyList());
    }

    @Override
    public Single<Long> createSession(String firstQuestion) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Flowable<String> ask(long sessionId, String question, @Nullable String imagePath) {
        return Flowable.error(new UnsupportedOperationException());
    }

    @Override
    public Completable saveAnswer(long sessionId, String answer, boolean incomplete) {
        return Completable.complete();
    }

    @Override
    public Flowable<String> regenerate(long sessionId, long assistantMessageId) {
        return Flowable.error(new UnsupportedOperationException());
    }

    @Override
    public Completable setFeedback(long messageId, int feedback) {
        return Completable.complete();
    }

    @Override
    public Completable deleteSession(long sessionId) {
        return Completable.complete();
    }

    @Override
    public Completable clearAll() {
        return Completable.complete();
    }

    @Override
    public Single<long[]> historyStats() {
        return Single.just(new long[]{0, 0});
    }

    @Override
    public Single<String> quickAnswer(String question) {
        return Single.error(new UnsupportedOperationException());
    }

    @Override
    public Completable discardIfNotSaving(long sessionId) {
        return Completable.complete();
    }
}
