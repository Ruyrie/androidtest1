package com.heban.app.data.remote.llm.dto;

import java.util.ArrayList;
import java.util.List;

/** 一条对话消息：纯文本时 content 为 String；带图片时为 List&lt;ContentPart&gt;。 */
public class ChatMessage {
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    /** system / user / assistant */
    public String role;
    /** 纯文本用 String；带图片用 List&lt;ContentPart&gt; */
    public Object content;

    public ChatMessage() {
    }

    public ChatMessage(String role, Object content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String text) {
        return new ChatMessage(ROLE_SYSTEM, text);
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(ROLE_USER, text);
    }

    public static ChatMessage assistant(String text) {
        return new ChatMessage(ROLE_ASSISTANT, text);
    }

    /** 带图片的用户消息：先图片后文字（与 9.2 的识图请求示例一致） */
    public static ChatMessage userWithImage(String imageDataUrl, String text) {
        List<ContentPart> parts = new ArrayList<>();
        parts.add(ContentPart.image(imageDataUrl));
        parts.add(ContentPart.text(text));
        return new ChatMessage(ROLE_USER, parts);
    }

    /** 文本内容（带图片时取其中的文字部分），用于测试与日志 */
    public String textContent() {
        if (content instanceof String) return (String) content;
        if (content instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object o : (List<?>) content) {
                if (o instanceof ContentPart && ContentPart.TYPE_TEXT.equals(((ContentPart) o).type)) {
                    sb.append(((ContentPart) o).text);
                }
            }
            return sb.toString();
        }
        return "";
    }
}
