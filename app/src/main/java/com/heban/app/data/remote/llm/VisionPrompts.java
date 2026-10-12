package com.heban.app.data.remote.llm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.model.ScanMode;
import com.heban.app.data.remote.llm.dto.ChatMessage;
import com.heban.app.data.remote.llm.dto.ChatRequest;

/** 拍照识别的提示词与请求（技术准备文档 9.2 / 9.5）：非流式，max_tokens 800，temperature 0.2。 */
public final class VisionPrompts {
    public static final int MAX_TOKENS = 800;
    public static final double TEMPERATURE = 0.2;

    /** VisionPrompts.SYSTEM：只输出一个 JSON 对象 */
    public static final String SYSTEM = "你是农作物病虫害识别助手。根据用户上传的照片判断问题。\n"
            + "只输出一个 JSON 对象，不要输出任何其他文字，也不要使用代码块标记。\n"
            + "JSON 结构如下（字段含义见说明）：\n"
            + "{\"image_ok\": true, \"image_issue\": \"\", \"crop\": \"黄瓜\",\n"
            + " \"top\": {\"name\": \"黄瓜霜霉病\", \"category\": \"卵菌病害\", \"part\": \"叶部\",\n"
            + "         \"confidence\": 0.92, \"severity\": \"中度\",\n"
            + "         \"evidence\": \"叶面受叶脉限制的黄色多角形斑，叶背有灰黑色霉层\",\n"
            + "         \"advice\": [\"上午通风排湿\", \"摘除病叶带出棚外\", \"发病初期选用对症药剂，按标签用量喷施\"],\n"
            + "         \"urgency_days\": 3},\n"
            + " \"others\": [{\"name\": \"黄瓜细菌性角斑病\", \"confidence\": 0.06},\n"
            + "            {\"name\": \"缺镁症\", \"confidence\": 0.02}]}\n"
            + "如果照片模糊、过暗或不是植物，image_ok 为 false，并在 image_issue 中说明原因。\n"
            + "如果植株健康，top.category 写「健康」。";

    private VisionPrompts() {
    }

    /** 识图请求：系统提示词 + [图片, 「识别模式：病虫害；用户选择的作物：黄瓜」] */
    @NonNull
    public static ChatRequest build(@NonNull String model, @NonNull String dataUrl, @Nullable ScanMode mode,
                                    @Nullable String cropHint) {
        ChatRequest req = new ChatRequest();
        req.model = model;
        req.stream = false;
        req.maxTokens = MAX_TOKENS;
        req.temperature = TEMPERATURE;
        req.messages.add(ChatMessage.system(SYSTEM));
        req.messages.add(ChatMessage.userWithImage(dataUrl, userText(mode, cropHint)));
        return req;
    }

    /** 「识别模式：病虫害；用户选择的作物：黄瓜」，没有选择作物时只写识别模式 */
    @NonNull
    public static String userText(@Nullable ScanMode mode, @Nullable String cropHint) {
        ScanMode m = mode == null ? ScanMode.PEST : mode;
        StringBuilder sb = new StringBuilder("识别模式：").append(m.label);
        if (cropHint != null && !cropHint.trim().isEmpty()) {
            sb.append("；用户选择的作物：").append(cropHint.trim());
        }
        return sb.toString();
    }
}
