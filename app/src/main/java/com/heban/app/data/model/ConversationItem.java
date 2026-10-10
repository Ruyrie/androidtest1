package com.heban.app.data.model;

import com.heban.app.data.local.entity.ConversationEntity;

import java.util.Objects;

/** S08 私信列表项：会话 + 对方显示信息。 */
public final class ConversationItem {
    public ConversationEntity conversation;
    /** 对方：农友、禾伴客服或禾伴小助手 */
    public Author peer;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversationItem)) return false;
        ConversationItem i = (ConversationItem) o;
        return conversation != null && i.conversation != null && conversation.id == i.conversation.id
                && conversation.unread == i.conversation.unread && conversation.lastAt == i.conversation.lastAt
                && Objects.equals(conversation.lastMessage, i.conversation.lastMessage)
                && Objects.equals(peer, i.peer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversation == null ? 0 : conversation.id);
    }
}
