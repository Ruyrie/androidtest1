package com.heban.app.ui.me.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.ChatSessionEntity;
import com.heban.app.data.model.ModelServiceConfig;
import com.heban.app.data.repository.AiRepository;
import com.heban.app.data.repository.ModelServiceRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

/** M11 ViewModel 测试用的假数据层。 */
final class AiSettingsFakes {
    private AiSettingsFakes() {
    }

    static final class Ai implements AiRepository {
        long[] stats = {12, 2516582};
        int clearCalls;

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
            return Single.just(1L);
        }

        @Override
        public Flowable<String> ask(long sessionId, String question, @Nullable String imagePath) {
            return Flowable.empty();
        }

        @Override
        public Completable saveAnswer(long sessionId, String answer, boolean incomplete) {
            return Completable.complete();
        }

        @Override
        public Flowable<String> regenerate(long sessionId, long assistantMessageId) {
            return Flowable.empty();
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
            return Completable.fromAction(() -> {
                clearCalls++;
                stats = new long[]{0, 0};
            });
        }

        @Override
        public Single<long[]> historyStats() {
            return Single.fromCallable(() -> stats);
        }

        @Override
        public Single<String> quickAnswer(String question) {
            return Single.just("");
        }

        @Override
        public Completable discardIfNotSaving(long sessionId) {
            return Completable.complete();
        }
    }

    static final class Models implements ModelServiceRepository {
        final BehaviorProcessor<ModelServiceConfig> config = BehaviorProcessor.createDefault(new ModelServiceConfig());
        final List<ModelServiceConfig> saved = new ArrayList<>();
        final List<ModelServiceConfig> tested = new ArrayList<>();
        int clears;
        boolean builtIn = true;
        @Nullable Throwable testError;

        @Override
        public Flowable<ModelServiceConfig> observeUserConfig() {
            return config;
        }

        @Override
        public Completable save(ModelServiceConfig c) {
            return Completable.fromAction(() -> {
                saved.add(c);
                ModelServiceConfig stored = new ModelServiceConfig();
                stored.enabled = true;
                stored.provider = c.provider;
                stored.baseUrl = c.baseUrl;
                stored.chatModel = c.chatModel;
                stored.visionModel = c.visionModel;
                stored.apiKeyLast4 = c.apiKey == null || c.apiKey.isEmpty()
                        ? config.getValue().apiKeyLast4 : c.apiKey.substring(c.apiKey.length() - 4);
                config.onNext(stored);
            });
        }

        @Override
        public Completable clear() {
            return Completable.fromAction(() -> {
                clears++;
                config.onNext(new ModelServiceConfig());
            });
        }

        @Override
        public Single<String> test(ModelServiceConfig c) {
            tested.add(c);
            return testError != null ? Single.error(testError) : Single.just(c.chatModel);
        }

        @Override
        public boolean hasBuiltInKey() {
            return builtIn;
        }
    }

    static final class Prefs implements AiSettingsPrefs {
        final BehaviorProcessor<String> style = BehaviorProcessor.createDefault(STYLE_DETAIL);
        final Map<Flag, BehaviorProcessor<Boolean>> flags = new EnumMap<>(Flag.class);

        Prefs() {
            for (Flag f : Flag.values()) flags.put(f, BehaviorProcessor.createDefault(true));
        }

        @Override
        public Flowable<String> observeStyle() {
            return style;
        }

        @Override
        public Completable setStyle(@NonNull String s) {
            return Completable.fromAction(() -> style.onNext(s));
        }

        @Override
        public Flowable<Boolean> observeFlag(@NonNull Flag flag) {
            return flags.get(flag);
        }

        @Override
        public Completable setFlag(@NonNull Flag flag, boolean value) {
            return Completable.fromAction(() -> flags.get(flag).onNext(value));
        }
    }
}
