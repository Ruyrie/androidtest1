package com.heban.app.data.remote.llm.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** OpenAI 兼容 Chat Completions 请求体（技术准备文档 9.2）。为 null 的字段不会序列化。 */
public class ChatRequest {
    public String model;
    public List<ChatMessage> messages = new ArrayList<>();
    public boolean stream;
    @SerializedName("max_tokens") public Integer maxTokens;
    public Double temperature;
    /**
     * DeepSeek 思考模式开关：DeepSeek 默认开启思考模式，本项目默认不开启，
     * 请求 DeepSeek 时由 LlmClient 设为 {"type": "disabled"}；其他服务保持为 null（不发送）。
     */
    public Thinking thinking;

    public static class Thinking {
        public String type;

        public Thinking(String type) {
            this.type = type;
        }

        public static Thinking disabled() {
            return new Thinking("disabled");
        }
    }
}
