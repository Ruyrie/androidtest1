package com.heban.app.data.remote.llm.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 流式数据中的一段（choices[].delta），也用于非流式响应（choices[].message）。
 * 思考模式的 reasoning_content 不解析（界面不显示）。
 */
public class ChatChunk {
    public String model;
    public List<Choice> choices;
    /** 部分服务在流中或 200 响应里用 error 对象报告错误 */
    public ErrorBody error;

    public static class Choice {
        public Delta delta;
        /** 非流式响应使用 */
        public Message message;
        @SerializedName("finish_reason") public String finishReason;
    }

    public static class Delta {
        public String content;
    }

    public static class Message {
        public String content;
    }

    /** {"error": {"message": "...", "type": "...", "code": "..."}} */
    public static class ErrorBody {
        public String message;
        public String type;
        public Object code;
    }
}
