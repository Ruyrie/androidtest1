package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.ChatSessionEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * AI 农技助手（D06 / M11，技术准备文档 9.3–9.4）。按当前账号隔离；未登录时写操作抛出 NOT_LOGGED_IN。
 * 系统提示词按 M11 设置（ai.style / ai.use_crops / ai.use_weather）组装，历史最多带 6 轮，单条截断 2000 字。
 * 未配置任何可用的 Key 时 ask() 直接以 BusinessException 结束（只显示错误提示）。
 */
public interface AiRepository {
    Flowable<List<ChatSessionEntity>> observeSessions();

    Flowable<List<ChatMessageEntity>> observeMessages(long sessionId);

    /** 新建会话，title 取第一个问题前 20 字；返回会话 id */
    Single<Long> createSession(String firstQuestion);

    /**
     * 提问：先写入用户消息（imagePath 为附带图片），再流式返回回答片段。
     * 带图片时改用识图模型非流式回答（一次发射全文）。回答结束后由调用方 saveAnswer 保存。
     */
    Flowable<String> ask(long sessionId, String question, @Nullable String imagePath);

    /** 保存回答；incomplete = true 表示中断（失败或停止生成） */
    Completable saveAnswer(long sessionId, String answer, boolean incomplete);

    /** 「重新生成」：删除该条回答后用它前面的用户消息重新提问 */
    Flowable<String> regenerate(long sessionId, long assistantMessageId);

    Completable setFeedback(long messageId, int feedback);

    Completable deleteSession(long sessionId);

    /** M11「清空全部对话」 */
    Completable clearAll();

    /** M11「12 个对话 · 占用 2.4 MB」：会话数与占用字节（消息文本 + 图片文件） */
    Single<long[]> historyStats();

    /**
     * 一次性（非流式）回答，不写入会话：S03 / S05「AI 参考答案」使用。系统提示词同 ask()，
     * 不附带用户的作物与天气信息。
     */
    Single<String> quickAnswer(String question);

    /** M11 关闭「保存对话到本机」时，离开 D06 后删除该会话 */
    Completable discardIfNotSaving(long sessionId);
}
