package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.Gson;
import com.heban.app.BuildConfig;
import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.ScanRecordEntity;
import com.heban.app.data.model.ModelServiceConfig;
import com.heban.app.data.model.ScanMode;
import com.heban.app.data.model.ScanOutcome;
import com.heban.app.data.model.ScanResult;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.llm.dto.ChatRequest;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/** 三个大模型 Repository 中不依赖 Room / DataStore 的规则 */
public class LlmRepositoriesTest {
    private final Gson gson = new Gson();

    // —— 模型服务设置（表 9-5） ——

    @Test
    public void normalize_presetProvidersUsePresetUrlAndModels() {
        ModelServiceConfig in = new ModelServiceConfig();
        in.provider = ModelServiceConfig.PROVIDER_DEEPSEEK;
        in.baseUrl = "https://wrong.example.com";
        in.chatModel = "";
        in.visionModel = " ";
        ModelServiceConfig c = LocalModelServiceRepository.normalize(in);
        assertEquals("https://api.deepseek.com", c.baseUrl);
        assertEquals(BuildConfig.DEEPSEEK_CHAT_MODEL, c.chatModel);
        assertEquals("", c.visionModel);
        assertTrue(c.enabled);

        in.provider = ModelServiceConfig.PROVIDER_DASHSCOPE;
        in.chatModel = "qwen-max";
        c = LocalModelServiceRepository.normalize(in);
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", c.baseUrl);
        assertEquals("qwen-max", c.chatModel);
    }

    @Test
    public void normalize_customStripsSlash_andValidates() {
        ModelServiceConfig in = new ModelServiceConfig();
        in.provider = ModelServiceConfig.PROVIDER_CUSTOM;
        in.baseUrl = " https://llm.example.com/v1/ ";
        in.chatModel = " glm-4 ";
        in.visionModel = "glm-4v";
        ModelServiceConfig c = LocalModelServiceRepository.normalize(in);
        assertEquals("https://llm.example.com/v1", c.baseUrl);
        assertEquals("glm-4", c.chatModel);
        assertEquals("glm-4v", c.visionModel);

        in.baseUrl = "llm.example.com";
        expectError(in, LocalModelServiceRepository.ERROR_INVALID_BASE_URL);
        in.baseUrl = "https://llm.example.com";
        in.chatModel = "";
        expectError(in, LocalModelServiceRepository.ERROR_NO_CHAT_MODEL);
    }

    private static void expectError(ModelServiceConfig in, String code) {
        try {
            LocalModelServiceRepository.normalize(in);
            fail("应当报错 " + code);
        } catch (BusinessException e) {
            assertEquals(code, e.code);
        }
    }

    @Test
    public void toConfig_readsBackWithoutPlainKey() {
        ModelServiceConfig c = LocalModelServiceRepository.toConfig(true, "custom", "https://llm.example.com/v1",
                "glm-4", "", "sk-abcdef1234");
        assertTrue(c.enabled);
        assertEquals("custom", c.provider);
        assertEquals("https://llm.example.com/v1", c.baseUrl);
        assertEquals("glm-4", c.chatModel);
        assertEquals("", c.visionModel);
        assertNull(c.apiKey);
        assertEquals("1234", c.apiKeyLast4);
    }

    @Test
    public void toConfig_nothingSaved_isDisabledDeepSeekPreset() {
        ModelServiceConfig c = LocalModelServiceRepository.toConfig(false, "deepseek", "", "", "", "");
        assertFalse(c.enabled);
        assertEquals("https://api.deepseek.com", c.baseUrl);
        assertEquals(BuildConfig.DEEPSEEK_CHAT_MODEL, c.chatModel);
        assertEquals(BuildConfig.DEEPSEEK_VISION_MODEL, c.visionModel);
        assertNull(c.apiKeyLast4);
    }

    @Test
    public void testRequest_isHelloWithEightTokens() {
        ChatRequest req = LocalModelServiceRepository.testRequest("deepseek-v4-flash");
        assertFalse(req.stream);
        assertEquals(8, (int) req.maxTokens);
        assertEquals(1, req.messages.size());
        assertEquals("你好", req.messages.get(0).content);
        assertEquals("abc", LocalModelServiceRepository.last4("abc"));
        assertNull(LocalModelServiceRepository.last4(" "));
    }

    // —— 拍照识别 ——

    private static ScanResult result(String name, String category, String severity) {
        ScanResult r = new ScanResult();
        r.top = new ScanResult.Top();
        r.top.name = name;
        r.top.category = category;
        r.top.severity = severity;
        r.top.confidence = 0.92;
        return r;
    }

    @Test
    public void saveToCrop_recordContent() {
        ScanResult r = result("黄瓜霜霉病", "卵菌病害", "中度");
        FarmRecordEntity rec = LlmVisionRepository.farmRecordFor(r, "file:///data/files/scans/a.jpg", 7L, 1000L, gson);
        assertEquals(7L, rec.cropId);
        assertEquals("OTHER", rec.type);
        assertEquals(1000L, rec.date);
        assertEquals("拍照识别：黄瓜霜霉病（中度）", rec.material);
        assertEquals("[\"file:///data/files/scans/a.jpg\"]", rec.photos);
        assertEquals("拍照识别：缺镁症", LlmVisionRepository.materialFor(result("缺镁症", "生理性", "")));
        assertTrue(LlmVisionRepository.isHealthy(result("健康", "健康", "")));
        assertFalse(LlmVisionRepository.isHealthy(r));
    }

    @Test
    public void outcomeFromRecord() {
        ScanRecordEntity rec = new ScanRecordEntity();
        rec.id = 3;
        rec.imagePath = "file:///x.jpg";
        rec.mode = "CROP";
        rec.provider = "通义千问 VL";
        rec.cropId = 9L;
        rec.createdAt = 42;
        ScanOutcome o = LlmVisionRepository.toOutcome(rec, result("稗草", "杂草", ""));
        assertEquals(3, o.recordId);
        assertEquals(ScanMode.CROP, o.mode);
        assertEquals("通义千问 VL", o.provider);
        assertEquals(Long.valueOf(9), o.cropId);
        assertEquals(42, o.createdAt);
        assertEquals("稗草", o.result.top.name);
        assertNull(o.pestId);
        assertEquals(ScanMode.PEST, LlmVisionRepository.modeOf("???"));
        assertEquals(ScanMode.PEST, LlmVisionRepository.modeOf(null));
    }

    // —— AI 助手 ——

    private static ChatMessageEntity msg(long id, String role) {
        ChatMessageEntity m = new ChatMessageEntity();
        m.id = id;
        m.role = role;
        return m;
    }

    @Test
    public void regenerate_findsQuestionBeforeAnswer() {
        List<ChatMessageEntity> all = Arrays.asList(msg(1, "user"), msg(2, "assistant"), msg(3, "user"),
                msg(4, "assistant"));
        assertEquals(3, LlmAiRepository.previousUserMessage(all, all.get(3)).id);
        assertEquals(1, LlmAiRepository.previousUserMessage(all, all.get(1)).id);
        assertNull(LlmAiRepository.previousUserMessage(Arrays.asList(msg(5, "assistant")), msg(5, "assistant")));
    }
}
