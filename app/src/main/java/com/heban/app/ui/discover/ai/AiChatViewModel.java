package com.heban.app.ui.discover.ai;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.R;
import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.PestEntity;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.AiRepository;
import com.heban.app.data.repository.PestRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;
import com.heban.app.ui.crop.PhotoPickerFileStore;
import com.heban.app.ui.crop.PhotoPickerStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * D06 农技 AI 助手（技术准备文档 9.3 / 9.7 / 9.8）：
 * 回答片段按 60ms 合并后刷新气泡；「停止生成」、失败时保留已生成的部分并标记「回答未完成」；
 * 发送 throttleFirst(800ms)；离开页面时断开流式请求，并按「保存对话到本机」决定是否删除会话。
 * <p>
 * 刚发出的问题和正在生成的回答先显示在本地，等会话消息（Room）中出现对应的新消息后再换成数据库中的那条。
 */
public class AiChatViewModel extends BaseViewModel {
    static final long THROTTLE_MS = 800;
    static final long BUFFER_MS = 60;
    /** 附带的图片复制到 filesDir/chat/ */
    static final String IMAGE_DIR = "chat";

    private static final class Request {
        final String question;
        @Nullable final String image;
        final boolean fromInput;

        Request(String question, @Nullable String image, boolean fromInput) {
            this.question = question;
            this.image = image;
            this.fromInput = fromInput;
        }
    }

    private final AiRepository repo;
    private final PestRepository pestRepo;
    private final PhotoPickerStore photos;
    private final Scheduler timeScheduler;

    public final MutableLiveData<List<AiChatItem>> items =
            new MutableLiveData<>(Collections.singletonList(AiChatItem.welcome()));
    public final MutableLiveData<Boolean> streaming = new MutableLiveData<>(false);
    /** 输入框（双向绑定） */
    public final MutableLiveData<String> input = new MutableLiveData<>("");
    /** 待发送的图片（file:// 路径） */
    public final MutableLiveData<String> attachedImage = new MutableLiveData<>(null);
    /** 输入框有文字 */
    public final MediatorLiveData<Boolean> canSend = new MediatorLiveData<>();
    /** 发出新问题：列表滚动到底部 */
    public final SingleLiveEvent<Boolean> scrollToEnd = new SingleLiveEvent<>();
    public final SingleLiveEvent<Integer> toastRes = new SingleLiveEvent<>();

    private final PublishSubject<Request> sendRequests = PublishSubject.create();
    private boolean started;
    private long sessionId;
    @NonNull private List<ChatMessageEntity> messages = Collections.emptyList();
    @NonNull private List<PestEntity> pests = Collections.emptyList();
    @Nullable private Disposable messagesSub;
    @Nullable private Disposable stream;

    // 本地显示的问题（未出现在会话消息中之前）
    @Nullable private String pendingQuestion;
    @Nullable private String pendingImage;
    private long userIdBeforeSend;
    // 本地显示的回答（生成中或等待保存）
    private boolean answerPending;
    private boolean answerIncomplete;
    private long assistantIdBeforeSend;
    /** 收到的全部片段（流式线程写入） */
    private final StringBuffer received = new StringBuffer();
    /** 已显示的文字（按 60ms 刷新） */
    @NonNull private String shownAnswer = "";
    /** 「重新生成」时先隐藏原回答 */
    private long hiddenMessageId;

    public AiChatViewModel(AppContainer c) {
        this(c, c.aiRepository, c.pestRepository, new PhotoPickerFileStore(c.app), Schedulers.computation());
    }

    AiChatViewModel(@Nullable AppContainer c, @NonNull AiRepository repo, @NonNull PestRepository pestRepo,
                    @NonNull PhotoPickerStore photos, @NonNull Scheduler timeScheduler) {
        super(c);
        this.repo = repo;
        this.pestRepo = pestRepo;
        this.photos = photos;
        this.timeScheduler = timeScheduler;
        canSend.setValue(false);
        canSend.addSource(input, s -> canSend.setValue(s != null && !s.trim().isEmpty()));
        add(sendRequests.throttleFirst(THROTTLE_MS, TimeUnit.MILLISECONDS, timeScheduler)
                .subscribe(this::doSend, e -> { }));
    }

    /**
     * @param initialSession 打开的历史会话（0 为新会话）
     * @param prompt         新会话中自动发送一次的问题（D03 / D05 / C02「问 AI」），可为空
     */
    public void start(long initialSession, @Nullable String prompt) {
        if (started) return;
        started = true;
        add(pestRepo.search(null, null, null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    pests = list == null ? Collections.emptyList() : list;
                    rebuild();
                }, e -> { }));
        if (initialSession > 0) bindSession(initialSession);
        rebuild();
        if (initialSession <= 0 && prompt != null && !prompt.trim().isEmpty()) ask(prompt);
    }

    public long sessionId() {
        return sessionId;
    }

    public boolean isStreaming() {
        return stream != null && !stream.isDisposed();
    }

    // ---------- 发送 ----------

    /** 发送输入框中的问题（及附带图片） */
    public void send() {
        String q = input.getValue() == null ? "" : input.getValue().trim();
        if (q.isEmpty() || isStreaming()) return;
        sendRequests.onNext(new Request(q, attachedImage.getValue(), true));
    }

    /** 发送指定问题（建议问题、自动带入的问题），不影响输入框 */
    public void ask(@Nullable String question) {
        if (question == null || question.trim().isEmpty() || isStreaming()) return;
        sendRequests.onNext(new Request(question.trim(), null, false));
    }

    private void doSend(Request r) {
        if (isStreaming()) return;
        if (r.fromInput) {
            input.setValue("");
            attachedImage.setValue(null);
        }
        pendingQuestion = r.question;
        pendingImage = r.image;
        userIdBeforeSend = maxId(ChatMessageEntity.ROLE_USER);
        Single<Long> session = sessionId > 0
                ? Single.just(sessionId)
                : repo.createSession(r.question)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(this::bindSession);
        consume(session.flatMapPublisher(id -> repo.ask(id, r.question, r.image)));
        scrollToEnd.setValue(true);
    }

    /** 「重新生成」：删除这条回答后用前面的问题重新提问 */
    public void regenerate(@NonNull AiChatItem item) {
        if (isStreaming() || item.id <= 0 || sessionId <= 0) return;
        hiddenMessageId = item.id;
        consume(repo.regenerate(sessionId, item.id));
        scrollToEnd.setValue(true);
    }

    private void consume(Flowable<String> source) {
        received.setLength(0);
        shownAnswer = "";
        answerPending = true;
        answerIncomplete = false;
        assistantIdBeforeSend = maxId(ChatMessageEntity.ROLE_ASSISTANT);
        stream = source
                .doOnNext(received::append)
                .buffer(BUFFER_MS, TimeUnit.MILLISECONDS, timeScheduler)
                .filter(parts -> !parts.isEmpty())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(parts -> {
                    shownAnswer = received.toString();
                    rebuild();
                }, e -> finishAnswer(true, e), () -> finishAnswer(false, null));
        add(stream);
        streaming.setValue(isStreaming());
        rebuild();
    }

    /** 「停止生成」：保留已生成的部分，标记回答未完成 */
    public void stop() {
        if (!isStreaming()) return;
        Disposable s = stream;
        s.dispose();
        disposables.delete(s);
        finishAnswer(true, null);
    }

    private void finishAnswer(boolean incomplete, @Nullable Throwable error) {
        stream = null;
        streaming.setValue(false);
        String text = received.toString();
        shownAnswer = text;
        if (error != null) toast.setValue(AppError.from(error).message);
        if (error != null && sessionId <= 0 && pendingQuestion != null) {
            // 会话没有建立（如未登录）：问题放回输入框
            String cur = input.getValue();
            if (cur == null || cur.trim().isEmpty()) input.setValue(pendingQuestion);
            if (attachedImage.getValue() == null) attachedImage.setValue(pendingImage);
            pendingQuestion = null;
            pendingImage = null;
        }
        if (text.trim().isEmpty()) {
            // 没有任何内容：不保存空回答
            answerPending = false;
            hiddenMessageId = 0;
            if (error == null && !incomplete) toastRes.setValue(R.string.ai_chat_empty_answer);
            rebuild();
            return;
        }
        answerIncomplete = incomplete;
        rebuild();
        if (sessionId <= 0) return;
        add(repo.saveAnswer(sessionId, text, incomplete)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> { }, e -> toast.setValue(AppError.from(e).message)));
    }

    // ---------- 会话 ----------

    private void bindSession(long id) {
        sessionId = id;
        if (messagesSub != null) {
            messagesSub.dispose();
            disposables.delete(messagesSub);
        }
        messagesSub = repo.observeMessages(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMessages, e -> toast.setValue(AppError.from(e).message));
        add(messagesSub);
    }

    private void onMessages(@Nullable List<ChatMessageEntity> list) {
        int before = messages.size();
        messages = list == null ? Collections.emptyList() : list;
        if (pendingQuestion != null && maxId(ChatMessageEntity.ROLE_USER) > userIdBeforeSend) {
            pendingQuestion = null;
            pendingImage = null;
        }
        if (answerPending && !isStreaming() && maxId(ChatMessageEntity.ROLE_ASSISTANT) > assistantIdBeforeSend) {
            answerPending = false;
            hiddenMessageId = 0;
        }
        rebuild();
        if (messages.size() > before) scrollToEnd.setValue(true);
    }

    /** 打开历史会话（0 为新对话）；当前会话按设置决定是否删除 */
    public void openSession(long id) {
        if (id > 0 && id == sessionId) return;
        leaveSession();
        if (id > 0) {
            bindSession(id);
        } else {
            sessionId = 0;
        }
        rebuild();
    }

    /** 历史会话中删除了某个会话；是当前会话时回到新对话 */
    public void onSessionDeleted(long id) {
        if (id <= 0 || id != sessionId) return;
        if (isStreaming()) {
            Disposable s = stream;
            s.dispose();
            disposables.delete(s);
            stream = null;
            streaming.setValue(false);
        }
        resetSessionState();
        sessionId = 0;
        rebuild();
    }

    private void leaveSession() {
        long old = sessionId;
        String partial = received.toString();
        boolean wasStreaming = isStreaming();
        if (wasStreaming) {
            Disposable s = stream;
            s.dispose();
            disposables.delete(s);
            stream = null;
            streaming.setValue(false);
        }
        resetSessionState();
        if (old > 0) closeSession(old, wasStreaming ? partial : null);
    }

    private void resetSessionState() {
        if (messagesSub != null) {
            messagesSub.dispose();
            disposables.delete(messagesSub);
            messagesSub = null;
        }
        messages = Collections.emptyList();
        pendingQuestion = null;
        pendingImage = null;
        answerPending = false;
        answerIncomplete = false;
        hiddenMessageId = 0;
        received.setLength(0);
        shownAnswer = "";
    }

    /** 离开会话：先保存中断的回答，再按「保存对话到本机」决定是否删除（不随页面取消） */
    private void closeSession(long id, @Nullable String partial) {
        Completable save = partial != null && !partial.trim().isEmpty()
                ? repo.saveAnswer(id, partial, true) : Completable.complete();
        //noinspection ResultOfMethodCallIgnored
        save.onErrorComplete()
                .andThen(repo.discardIfNotSaving(id))
                .subscribe(() -> { }, e -> { });
    }

    // ---------- 回答操作 ----------

    public void toggleLike(@NonNull AiChatItem item) {
        if (item.id <= 0) return;
        add(repo.setFeedback(item.id, item.liked ? 0 : 1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> { }, e -> toast.setValue(AppError.from(e).message)));
    }

    // ---------- 图片与语音 ----------

    public void attachImage(@Nullable Uri uri) {
        if (uri == null) return;
        add(photos.copy(uri, IMAGE_DIR)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(path -> {
                    String old = attachedImage.getValue();
                    if (old != null) photos.delete(Collections.singletonList(old));
                    attachedImage.setValue(path);
                }, e -> toastRes.setValue(R.string.ai_chat_photo_failed)));
    }

    public void removeImage() {
        String old = attachedImage.getValue();
        if (old == null) return;
        photos.delete(Collections.singletonList(old));
        attachedImage.setValue(null);
    }

    /** 语音识别结果追加到输入框 */
    public void appendVoice(@Nullable String text) {
        if (text == null || text.trim().isEmpty()) return;
        String cur = input.getValue() == null ? "" : input.getValue();
        input.setValue(cur + text.trim());
    }

    // ---------- 列表 ----------

    private long maxId(String role) {
        long max = 0;
        for (ChatMessageEntity m : messages) if (role.equals(m.role) && m.id > max) max = m.id;
        return max;
    }

    private void rebuild() {
        List<AiChatItem> out = new ArrayList<>(messages.size() + 3);
        out.add(AiChatItem.welcome());
        boolean busy = isStreaming() || answerPending;
        long lastAssistant = 0;
        for (ChatMessageEntity m : messages) {
            if (m.id != hiddenMessageId && ChatMessageEntity.ROLE_ASSISTANT.equals(m.role)) lastAssistant = m.id;
        }
        for (ChatMessageEntity m : messages) {
            if (m.id == hiddenMessageId) continue;
            if (ChatMessageEntity.ROLE_ASSISTANT.equals(m.role)) {
                boolean incomplete = ChatMessageEntity.STATUS_INCOMPLETE.equals(m.status);
                PestEntity p = incomplete ? null : AiChatPests.match(m.content, pests);
                out.add(AiChatItem.assistant(m.id, m.content, false, incomplete, m.feedback == 1,
                        !busy && m.id == lastAssistant, p == null ? 0 : p.id,
                        p == null ? null : AiChatPests.shortName(p)));
            } else {
                out.add(AiChatItem.user(m.id, m.content, m.imagePath));
            }
        }
        if (pendingQuestion != null) {
            out.add(AiChatItem.user(AiChatItem.ID_PENDING_QUESTION, pendingQuestion, pendingImage));
        }
        if (answerPending) {
            boolean live = isStreaming();
            out.add(AiChatItem.assistant(AiChatItem.ID_PENDING_ANSWER, shownAnswer, live,
                    !live && answerIncomplete, false, false, 0, null));
        }
        items.setValue(out);
    }

    @Override
    protected void onCleared() {
        boolean wasStreaming = isStreaming();
        String partial = received.toString();
        long sid = sessionId;
        super.onCleared();
        String img = attachedImage.getValue();
        if (img != null) photos.delete(Collections.singletonList(img));
        if (sid > 0) closeSession(sid, wasStreaming ? partial : null);
    }
}
