package com.heban.app.ui.discover.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** D06 列表中的一项：欢迎语、用户消息或助手回答（含正在生成、等待保存的回答）。 */
public final class AiChatItem {
    public static final int WELCOME = 0;
    public static final int USER = 1;
    public static final int ASSISTANT = 2;

    /** 发出后、尚未出现在会话消息中的问题 */
    public static final long ID_PENDING_QUESTION = -1;
    /** 正在生成或等待保存的回答 */
    public static final long ID_PENDING_ANSWER = -2;

    public final int kind;
    /** 消息 id；欢迎语为 0，未保存的为负数 */
    public final long id;
    @NonNull public final String text;
    @Nullable public final String imagePath;
    /** 正在流式输出 */
    public final boolean streaming;
    /** 「回答未完成」 */
    public final boolean incomplete;
    public final boolean liked;
    /** 已保存的回答可复制、点赞（未保存的只显示文字） */
    public final boolean saved;
    /** 最后一条回答显示「重新生成」 */
    public final boolean canRegenerate;
    /** 匹配到的百科条目 id（0 表示没有） */
    public final long pestId;
    /** 百科标签中的病名，如「霜霉病」 */
    @Nullable public final String pestName;

    private AiChatItem(int kind, long id, @NonNull String text, @Nullable String imagePath, boolean streaming,
                       boolean incomplete, boolean liked, boolean saved, boolean canRegenerate, long pestId,
                       @Nullable String pestName) {
        this.kind = kind;
        this.id = id;
        this.text = text;
        this.imagePath = imagePath;
        this.streaming = streaming;
        this.incomplete = incomplete;
        this.liked = liked;
        this.saved = saved;
        this.canRegenerate = canRegenerate;
        this.pestId = pestId;
        this.pestName = pestName;
    }

    public static AiChatItem welcome() {
        return new AiChatItem(WELCOME, 0, "", null, false, false, false, false, false, 0, null);
    }

    public static AiChatItem user(long id, @NonNull String text, @Nullable String imagePath) {
        return new AiChatItem(USER, id, text, imagePath, false, false, false, id > 0, false, 0, null);
    }

    public static AiChatItem assistant(long id, @NonNull String text, boolean streaming, boolean incomplete,
                                       boolean liked, boolean canRegenerate, long pestId,
                                       @Nullable String pestName) {
        return new AiChatItem(ASSISTANT, id, text, null, streaming, incomplete, liked, id > 0,
                canRegenerate, pestId, pestName);
    }

    public boolean sameItem(@NonNull AiChatItem o) {
        return kind == o.kind && id == o.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AiChatItem)) return false;
        AiChatItem that = (AiChatItem) o;
        return kind == that.kind && id == that.id && streaming == that.streaming
                && incomplete == that.incomplete && liked == that.liked && saved == that.saved
                && canRegenerate == that.canRegenerate && pestId == that.pestId
                && text.equals(that.text) && Objects.equals(imagePath, that.imagePath)
                && Objects.equals(pestName, that.pestName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, id, text, imagePath, streaming, incomplete, liked, saved, canRegenerate,
                pestId, pestName);
    }
}
