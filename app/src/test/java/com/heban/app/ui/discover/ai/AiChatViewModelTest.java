package com.heban.app.ui.discover.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.R;
import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.remote.BusinessException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;

public class AiChatViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AiChatFakes.Ai repo;
    private AiChatFakes.Pests pests;
    private AiChatFakes.Photos photos;
    private TestScheduler time;
    private AiChatViewModel vm;
    private final List<String> toasts = new ArrayList<>();
    private final List<Integer> toastRes = new ArrayList<>();

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        repo = new AiChatFakes.Ai();
        pests = new AiChatFakes.Pests();
        photos = new AiChatFakes.Photos();
        time = new TestScheduler();
        vm = new AiChatViewModel(null, repo, pests, photos, time);
        vm.toast.observeForever(toasts::add);
        vm.toastRes.observeForever(toastRes::add);
        vm.canSend.observeForever(x -> { });   // MediatorLiveData 需要观察者才会更新
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    private AiChatItem last() {
        List<AiChatItem> items = vm.items.getValue();
        return items.get(items.size() - 1);
    }

    private void type(String text) {
        vm.input.setValue(text);
    }

    private void advance(long ms) {
        time.advanceTimeBy(ms, TimeUnit.MILLISECONDS);
    }

    @Test
    public void newSession_showsWelcomeOnly() {
        vm.start(0, null);
        assertEquals(1, vm.items.getValue().size());
        assertEquals(AiChatItem.WELCOME, vm.items.getValue().get(0).kind);
        assertFalse(vm.canSend.getValue());
        type("  ");
        assertFalse(vm.canSend.getValue());
        type("番茄叶片发黄怎么办？");
        assertTrue(vm.canSend.getValue());
    }

    @Test
    public void streaming_accumulatesEvery60msAndSavesOnComplete() {
        vm.start(0, null);
        type("番茄叶片发黄怎么办？");
        vm.send();
        assertEquals(Arrays.asList("番茄叶片发黄怎么办？"), repo.created);
        assertEquals(Arrays.asList("番茄叶片发黄怎么办？"), repo.asked);
        assertEquals("", vm.input.getValue());
        assertTrue(vm.streaming.getValue());
        assertTrue(last().streaming);
        assertEquals("", last().text);

        repo.stream.onNext("从描述看");
        repo.stream.onNext("更像缺镁");
        assertEquals("", last().text);           // 60ms 内合并
        advance(60);
        assertEquals("从描述看更像缺镁", last().text);
        assertEquals(AiChatItem.ID_PENDING_ANSWER, last().id);

        repo.stream.onNext("\n- 叶面喷施");
        repo.stream.onComplete();
        assertFalse(vm.streaming.getValue());
        assertEquals(1, repo.saved.size());
        assertEquals("从描述看更像缺镁\n- 叶面喷施", repo.saved.get(0)[1]);
        assertEquals(false, repo.saved.get(0)[2]);
        // 保存后换成数据库中的消息：问题 + 回答
        List<AiChatItem> items = vm.items.getValue();
        assertEquals(3, items.size());
        assertEquals(AiChatItem.USER, items.get(1).kind);
        assertTrue(items.get(1).id > 0);
        assertTrue(last().id > 0);
        assertFalse(last().incomplete);
        assertTrue(last().canRegenerate);
    }

    @Test
    public void stop_keepsPartialAnswerAsIncomplete() {
        vm.start(0, null);
        type("十月适合种什么菜？");
        vm.send();
        repo.stream.onNext("十月可以种");
        advance(60);
        repo.stream.onNext("菠菜、");   // 未刷新的片段也要保存
        vm.stop();
        assertFalse(vm.streaming.getValue());
        assertFalse(repo.stream.hasSubscribers());
        assertEquals("十月可以种菠菜、", repo.saved.get(0)[1]);
        assertEquals(true, repo.saved.get(0)[2]);
        assertTrue(last().incomplete);
        assertTrue(last().canRegenerate);
    }

    @Test
    public void error_keepsPartialAndShowsMessage() {
        vm.start(0, null);
        type("番茄叶片发黄怎么办？");
        vm.send();
        repo.stream.onNext("部分回答");
        repo.stream.onError(new BusinessException("LLM", "模型服务余额不足"));
        assertEquals(Arrays.asList("模型服务余额不足"), toasts);
        assertEquals("部分回答", repo.saved.get(0)[1]);
        assertEquals(true, repo.saved.get(0)[2]);
        assertTrue(last().incomplete);
    }

    @Test
    public void errorWithoutText_savesNothing() {
        vm.start(0, null);
        type("番茄叶片发黄怎么办？");
        vm.send();
        repo.stream.onError(new BusinessException("NO_KEY", "未配置 API Key"));
        assertEquals(Arrays.asList("未配置 API Key"), toasts);
        assertTrue(repo.saved.isEmpty());
        assertEquals(AiChatItem.USER, last().kind);
        assertFalse(vm.streaming.getValue());
    }

    @Test
    public void sessionCreationFails_restoresQuestion() {
        repo.createError = new BusinessException("NOT_LOGGED_IN", "请先登录");
        vm.start(0, null);
        type("番茄叶片发黄怎么办？");
        vm.send();
        assertEquals("番茄叶片发黄怎么办？", vm.input.getValue());
        assertEquals(1, vm.items.getValue().size());
        assertEquals(Arrays.asList("请先登录"), toasts);
    }

    @Test
    public void emptyAnswer_showsHint() {
        vm.start(0, null);
        type("你好");
        vm.send();
        repo.stream.onComplete();
        assertTrue(repo.saved.isEmpty());
        assertEquals(Arrays.asList(R.string.ai_chat_empty_answer), toastRes);
    }

    @Test
    public void send_isThrottled800ms() {
        vm.start(0, null);
        type("第一个问题");
        vm.send();
        repo.stream.onComplete();
        type("第二个问题");
        vm.send();                       // 800ms 内：忽略
        assertEquals(1, repo.asked.size());
        advance(800);
        repo.stream = io.reactivex.rxjava3.processors.PublishProcessor.create();
        vm.send();
        assertEquals(Arrays.asList("第一个问题", "第二个问题"), repo.asked);
        assertEquals(1, repo.created.size());   // 同一个会话
    }

    @Test
    public void send_ignoredWhileStreaming() {
        vm.start(0, null);
        type("第一个问题");
        vm.send();
        advance(1000);
        type("第二个问题");
        vm.send();
        vm.ask("番茄叶片发黄怎么办？");
        assertEquals(1, repo.asked.size());
        assertEquals("第二个问题", vm.input.getValue());
    }

    @Test
    public void prompt_isSentOnceInNewSession() {
        vm.start(0, "黄瓜霜霉病怎么防治？");
        vm.start(0, "黄瓜霜霉病怎么防治？");
        assertEquals(Arrays.asList("黄瓜霜霉病怎么防治？"), repo.asked);
        assertEquals(Arrays.asList("黄瓜霜霉病怎么防治？"), repo.created);
    }

    @Test
    public void existingSession_showsMessagesAndPestChip() {
        pests.list.add(AiChatFakes.pest(9, "黄瓜霜霉病", "黄瓜、甜瓜"));
        repo.add(AiChatFakes.message(1, ChatMessageEntity.ROLE_USER, "黄瓜叶片有黄斑", ChatMessageEntity.STATUS_OK));
        repo.add(AiChatFakes.message(2, ChatMessageEntity.ROLE_ASSISTANT, "初步判断为 **黄瓜霜霉病**。",
                ChatMessageEntity.STATUS_OK));
        repo.nextId = 3;
        vm.start(1, "不会自动发送");
        assertTrue(repo.asked.isEmpty());
        assertEquals(1, vm.sessionId());
        AiChatItem answer = last();
        assertEquals(9, answer.pestId);
        assertEquals("霜霉病", answer.pestName);
        assertTrue(answer.saved);
    }

    @Test
    public void regenerate_hidesOldAnswerAndStreamsAgain() {
        repo.add(AiChatFakes.message(1, ChatMessageEntity.ROLE_USER, "问题", ChatMessageEntity.STATUS_OK));
        repo.add(AiChatFakes.message(2, ChatMessageEntity.ROLE_ASSISTANT, "旧回答", ChatMessageEntity.STATUS_INCOMPLETE));
        repo.nextId = 3;
        vm.start(1, null);
        vm.regenerate(last());
        assertEquals(Arrays.asList(2L), repo.regenerated);
        assertTrue(last().streaming);
        repo.stream.onNext("新回答");
        repo.stream.onComplete();
        assertEquals("新回答", last().text);
        assertFalse(last().incomplete);
        assertEquals(3, vm.items.getValue().size());
    }

    @Test
    public void onlyLastAnswerCanRegenerate() {
        repo.add(AiChatFakes.message(1, ChatMessageEntity.ROLE_USER, "问1", ChatMessageEntity.STATUS_OK));
        repo.add(AiChatFakes.message(2, ChatMessageEntity.ROLE_ASSISTANT, "答1", ChatMessageEntity.STATUS_OK));
        repo.add(AiChatFakes.message(3, ChatMessageEntity.ROLE_USER, "问2", ChatMessageEntity.STATUS_OK));
        repo.add(AiChatFakes.message(4, ChatMessageEntity.ROLE_ASSISTANT, "答2", ChatMessageEntity.STATUS_OK));
        vm.start(1, null);
        List<AiChatItem> items = vm.items.getValue();
        assertFalse(items.get(2).canRegenerate);
        assertTrue(items.get(4).canRegenerate);
    }

    @Test
    public void toggleLike() {
        repo.add(AiChatFakes.message(2, ChatMessageEntity.ROLE_ASSISTANT, "答", ChatMessageEntity.STATUS_OK));
        vm.start(1, null);
        vm.toggleLike(last());
        assertEquals(2, repo.feedback.get(0)[0]);
        assertEquals(1, repo.feedback.get(0)[1]);
    }

    @Test
    public void imageIsSentWithQuestion() {
        vm.start(0, null);
        vm.attachedImage.setValue("file:///data/files/chat/a.jpg");
        type("这是什么病？");
        vm.send();
        assertEquals("file:///data/files/chat/a.jpg", repo.askedImages.get(0));
        assertNull(vm.attachedImage.getValue());
        assertTrue(photos.deleted.isEmpty());
    }

    @Test
    public void removeImage_deletesCopy() {
        vm.start(0, null);
        vm.attachedImage.setValue("file:///data/files/chat/a.jpg");
        vm.removeImage();
        assertNull(vm.attachedImage.getValue());
        assertEquals(Arrays.asList("file:///data/files/chat/a.jpg"), photos.deleted);
    }

    @Test
    public void leaving_cancelsStreamSavesPartialAndDiscards() {
        vm.start(0, null);
        type("问题");
        vm.send();
        repo.stream.onNext("一半");
        vm.attachedImage.setValue("file:///data/files/chat/b.jpg");
        vm.onCleared();
        assertFalse(repo.stream.hasSubscribers());
        assertEquals("一半", repo.saved.get(0)[1]);
        assertEquals(true, repo.saved.get(0)[2]);
        assertEquals(Arrays.asList(1L), repo.discarded);
        assertEquals(Arrays.asList("file:///data/files/chat/b.jpg"), photos.deleted);
    }

    @Test
    public void openAnotherSession_discardsCurrent() {
        vm.start(0, null);
        type("问题");
        vm.send();
        repo.stream.onComplete();
        vm.openSession(0);
        assertEquals(Arrays.asList(1L), repo.discarded);
        assertEquals(0, vm.sessionId());
        assertEquals(1, vm.items.getValue().size());
    }

    @Test
    public void deletingCurrentSession_returnsToNewChat() {
        repo.add(AiChatFakes.message(1, ChatMessageEntity.ROLE_USER, "问", ChatMessageEntity.STATUS_OK));
        vm.start(1, null);
        vm.onSessionDeleted(1);
        assertEquals(0, vm.sessionId());
        assertEquals(1, vm.items.getValue().size());
        assertTrue(repo.discarded.isEmpty());
    }
}
