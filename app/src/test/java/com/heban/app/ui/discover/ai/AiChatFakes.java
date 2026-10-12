package com.heban.app.ui.discover.ai;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.ChatSessionEntity;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.repository.AiRepository;
import com.heban.app.data.repository.PestRepository;
import com.heban.app.ui.crop.PhotoPickerStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.processors.PublishProcessor;

/** D06 ViewModel 测试用的假数据层：消息写入内存，回答片段由测试手动发射。 */
final class AiChatFakes {
    private AiChatFakes() {
    }

    static ChatMessageEntity message(long id, String role, String content, String status) {
        ChatMessageEntity m = new ChatMessageEntity();
        m.id = id;
        m.sessionId = 1;
        m.role = role;
        m.content = content;
        m.status = status;
        return m;
    }

    static PestEntity pest(long id, String name, String cropTags) {
        PestEntity p = new PestEntity();
        p.id = id;
        p.name = name;
        p.cropTags = cropTags;
        return p;
    }

    static final class Ai implements AiRepository {
        final BehaviorProcessor<List<ChatMessageEntity>> messages =
                BehaviorProcessor.createDefault(new ArrayList<>());
        PublishProcessor<String> stream = PublishProcessor.create();
        final List<String> created = new ArrayList<>();
        final List<String> asked = new ArrayList<>();
        final List<String> askedImages = new ArrayList<>();
        final List<Object[]> saved = new ArrayList<>();
        final List<Long> regenerated = new ArrayList<>();
        final List<Long> discarded = new ArrayList<>();
        final List<long[]> feedback = new ArrayList<>();
        @Nullable Throwable createError;
        long nextId = 1;

        void add(ChatMessageEntity m) {
            List<ChatMessageEntity> l = new ArrayList<>(messages.getValue());
            l.add(m);
            messages.onNext(l);
        }

        @Override
        public Flowable<List<ChatSessionEntity>> observeSessions() {
            return Flowable.just(Collections.emptyList());
        }

        @Override
        public Flowable<List<ChatMessageEntity>> observeMessages(long sessionId) {
            return messages;
        }

        @Override
        public Single<Long> createSession(String firstQuestion) {
            if (createError != null) return Single.error(createError);
            created.add(firstQuestion);
            return Single.just(1L);
        }

        @Override
        public Flowable<String> ask(long sessionId, String question, @Nullable String imagePath) {
            asked.add(question);
            askedImages.add(imagePath);
            ChatMessageEntity m = message(nextId++, ChatMessageEntity.ROLE_USER, question, ChatMessageEntity.STATUS_OK);
            m.imagePath = imagePath;
            add(m);
            return stream;
        }

        @Override
        public Completable saveAnswer(long sessionId, String answer, boolean incomplete) {
            return Completable.fromAction(() -> {
                saved.add(new Object[]{sessionId, answer, incomplete});
                add(message(nextId++, ChatMessageEntity.ROLE_ASSISTANT, answer,
                        incomplete ? ChatMessageEntity.STATUS_INCOMPLETE : ChatMessageEntity.STATUS_OK));
            });
        }

        @Override
        public Flowable<String> regenerate(long sessionId, long assistantMessageId) {
            regenerated.add(assistantMessageId);
            List<ChatMessageEntity> l = new ArrayList<>();
            for (ChatMessageEntity m : messages.getValue()) if (m.id != assistantMessageId) l.add(m);
            messages.onNext(l);
            return stream;
        }

        @Override
        public Completable setFeedback(long messageId, int value) {
            return Completable.fromAction(() -> feedback.add(new long[]{messageId, value}));
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
            return Single.just("");
        }

        @Override
        public Completable discardIfNotSaving(long sessionId) {
            return Completable.fromAction(() -> discarded.add(sessionId));
        }
    }

    static final class Pests implements PestRepository {
        final List<PestEntity> list = new ArrayList<>();

        @Override
        public Flowable<List<PestEntity>> search(@Nullable String keyword, @Nullable String kind,
                                                 @Nullable String crop) {
            return Flowable.just(list);
        }

        @Override
        public Flowable<List<PestEntity>> seasonal(int month) {
            return Flowable.just(Collections.emptyList());
        }

        @Override
        public Flowable<Integer> count() {
            return Flowable.just(list.size());
        }

        @Override
        public Single<PestEntity> get(long id) {
            return Single.just(list.get(0));
        }

        @Override
        public Maybe<PestEntity> matchByName(String name) {
            return Maybe.empty();
        }

        @Override
        public Single<List<String>> cropOptions() {
            return Single.just(Collections.emptyList());
        }
    }

    static final class Photos implements PhotoPickerStore {
        final List<String> deleted = new ArrayList<>();
        int copies;

        @NonNull
        @Override
        public Single<String> copy(@NonNull Uri source, @NonNull String dir) {
            copies++;
            return Single.just("file:///data/files/" + dir + "/p" + copies + ".jpg");
        }

        @NonNull
        @Override
        public Single<List<String>> copyAll(@NonNull List<Uri> sources, @NonNull String dir) {
            return Single.just(Collections.emptyList());
        }

        @Override
        public void delete(@NonNull Collection<String> paths) {
            deleted.addAll(paths);
        }

        @Nullable
        @Override
        public Uri createCameraTarget() {
            return null;
        }

        @Override
        public void discardCameraTarget(@NonNull Uri target) {
        }
    }
}
