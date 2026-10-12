package com.heban.app.data.remote.llm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.BuildConfig;
import com.heban.app.data.local.prefs.TokenCipher;
import com.heban.app.data.model.ModelServiceConfig;
import com.heban.app.data.remote.AppError;

import org.junit.Test;

/** LlmConfigProvider 的优先级：用户配置 → BuildConfig 默认（DeepSeek）；识图备用固定为通义千问 VL */
public class LlmConfigProviderTest {
    private static final LlmConfigProvider.Defaults BUILT_IN = new LlmConfigProvider.Defaults(
            "ds-key", "deepseek-v4-flash", "deepseek-v4-flash-vision-exp", "qw-key", "qwen-vl-plus");
    private static final LlmConfigProvider.Defaults NO_KEYS = new LlmConfigProvider.Defaults(
            "", "deepseek-v4-flash", "deepseek-v4-flash-vision-exp", "", "qwen-vl-plus");

    private static LlmConfigProvider.UserSettings user(String provider, String baseUrl, String key, String chat,
                                                       String vision) {
        LlmConfigProvider.UserSettings u = new LlmConfigProvider.UserSettings();
        u.enabled = true;
        u.provider = provider;
        u.baseUrl = baseUrl;
        u.apiKey = key;
        u.chatModel = chat;
        u.visionModel = vision;
        return u;
    }

    private static LlmConfigProvider provider(LlmConfigProvider.UserSettings u, LlmConfigProvider.Defaults d) {
        return new LlmConfigProvider(() -> u, d);
    }

    @Test
    public void noUserConfig_usesBuiltInDeepSeek() {
        LlmConfigProvider p = provider(new LlmConfigProvider.UserSettings(), BUILT_IN);
        LlmConfig chat = p.chat();
        assertEquals("deepseek", chat.provider);
        assertEquals("DeepSeek", chat.displayName);
        assertEquals("https://api.deepseek.com", chat.baseUrl);
        assertEquals("https://api.deepseek.com/chat/completions", chat.chatCompletionsUrl());
        assertEquals("ds-key", chat.apiKey);
        assertEquals("deepseek-v4-flash", chat.model);

        LlmConfigProvider.VisionPair v = p.vision();
        assertEquals("deepseek-v4-flash-vision-exp", v.primary.model);
        assertEquals("DeepSeek", v.primary.displayName);
        assertEquals("通义千问 VL", v.backup.displayName);
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", v.backup.baseUrl);
        assertEquals("qwen-vl-plus", v.backup.model);
        assertEquals("qw-key", v.backup.apiKey);
    }

    @Test
    public void enabledUserConfig_winsForChat_andVisionWhenFilled() {
        LlmConfigProvider p = provider(user("custom", "https://llm.example.com/v1/", " my-key ", "glm-4", "glm-4v"),
                BUILT_IN);
        LlmConfig chat = p.chat();
        assertEquals("custom", chat.provider);
        assertEquals("https://llm.example.com/v1", chat.baseUrl);
        assertEquals("my-key", chat.apiKey);
        assertEquals("glm-4", chat.model);
        assertEquals("glm-4", chat.displayName);
        LlmConfigProvider.VisionPair v = p.vision();
        assertEquals("glm-4v", v.primary.model);
        assertEquals("glm-4v", v.primary.displayName);
        assertEquals("my-key", v.primary.apiKey);
        assertEquals("通义千问 VL", v.backup.displayName);
    }

    @Test
    public void userConfigWithoutVisionModel_usesDefaultDeepSeekVision() {
        LlmConfigProvider p = provider(user("custom", "https://llm.example.com/v1", "my-key", "glm-4", ""), BUILT_IN);
        LlmConfig primary = p.visionPrimary();
        assertEquals("https://api.deepseek.com", primary.baseUrl);
        assertEquals("ds-key", primary.apiKey);
        assertEquals("deepseek-v4-flash-vision-exp", primary.model);

        // 用户的服务就是 DeepSeek：用用户自己的 Key
        LlmConfigProvider ds = provider(user("deepseek", "https://api.deepseek.com", "user-ds", "deepseek-v4-flash",
                null), NO_KEYS);
        assertEquals("user-ds", ds.visionPrimary().apiKey);
        assertEquals("deepseek-v4-flash-vision-exp", ds.visionPrimary().model);
        assertEquals("DeepSeek", ds.visionPrimary().displayName);
    }

    @Test
    public void dashscopeUserConfig_displayNames() {
        LlmConfigProvider p = provider(user("dashscope", "https://dashscope.aliyuncs.com/compatible-mode/v1", "k",
                "qwen-plus", "qwen-vl-max"), BUILT_IN);
        assertEquals("通义千问", p.chat().displayName);
        assertEquals("通义千问 VL", p.visionPrimary().displayName);
    }

    @Test
    public void disabledOrIncompleteUserConfig_fallsBackToBuiltIn() {
        LlmConfigProvider.UserSettings disabled = user("custom", "https://llm.example.com", "k", "m", "");
        disabled.enabled = false;
        assertEquals("ds-key", provider(disabled, BUILT_IN).chat().apiKey);
        assertEquals("ds-key", provider(user("custom", "https://llm.example.com", "", "m", ""), BUILT_IN).chat().apiKey);
        assertEquals("ds-key", provider(user("custom", "not a url", "k", "m", ""), BUILT_IN).chat().apiKey);
        assertEquals("ds-key", provider(user("custom", "https://llm.example.com", "k", " ", ""), BUILT_IN).chat().apiKey);
    }

    @Test
    public void noKeysAnywhere_isNull_andErrorTellsWhereToConfigure() {
        LlmConfigProvider p = provider(new LlmConfigProvider.UserSettings(), NO_KEYS);
        assertNull(p.chat());
        assertNull(p.visionPrimary());
        assertNull(p.visionBackup());
        assertNull(p.vision());
        AppError e = AppError.from(LlmConfigProvider.notConfigured());
        assertEquals(AppError.Type.BUSINESS, e.type);
        assertTrue(e.message.contains("「AI 助手设置 → 模型服务」"));
    }

    @Test
    public void onlyDashscopeKey_backupBecomesPrimary() {
        LlmConfigProvider p = provider(new LlmConfigProvider.UserSettings(),
                new LlmConfigProvider.Defaults("", "c", "v", "qw-key", "qwen-vl-plus"));
        assertNull(p.chat());
        LlmConfigProvider.VisionPair v = p.vision();
        assertEquals("通义千问 VL", v.primary.displayName);
        assertNull(v.backup);
    }

    @Test
    public void backupSameAsPrimary_notRepeated() {
        LlmConfigProvider p = provider(user("dashscope", "https://dashscope.aliyuncs.com/compatible-mode/v1/",
                "qw-key", "qwen-plus", "qwen-vl-plus"), BUILT_IN);
        assertNull(p.vision().backup);
    }

    @Test
    public void readSettings_decryptFailure_meansNoKey() {
        TokenCipher broken = new TokenCipher() {
            @Override
            public String encrypt(String plain) {
                return plain;
            }

            @Override
            public String decrypt(String encoded) throws Exception {
                throw new java.security.GeneralSecurityException("keystore reset");
            }
        };
        assertEquals("", LlmConfigProvider.decryptOrEmpty(broken, "abc"));
        assertEquals("", LlmConfigProvider.decryptOrEmpty(broken, ""));
    }

    @Test
    public void normalizeBaseUrl() {
        assertEquals("https://api.deepseek.com", LlmConfig.normalizeBaseUrl(" https://api.deepseek.com/ "));
        assertEquals("https://api.deepseek.com", LlmConfig.normalizeBaseUrl("https://api.deepseek.com//"));
        assertEquals("https://x.com/v1", LlmConfig.normalizeBaseUrl("https://x.com/v1/chat/completions"));
        assertTrue(LlmConfig.isValidBaseUrl("http://192.168.1.2:8080/v1"));
        assertFalse(LlmConfig.isValidBaseUrl("api.deepseek.com"));
        assertFalse(LlmConfig.isValidBaseUrl("https://"));
        assertFalse(LlmConfig.isValidBaseUrl(null));
        assertTrue(LlmConfig.isDeepSeekUrl("https://api.deepseek.com/"));
        assertFalse(LlmConfig.isDeepSeekUrl("https://api.deepseek.com.evil.cn"));
    }

    @Test
    public void presets() {
        ModelServiceConfig ds = LlmPresets.preset("deepseek");
        assertEquals("https://api.deepseek.com", ds.baseUrl);
        assertEquals(BuildConfig.DEEPSEEK_CHAT_MODEL, ds.chatModel);
        assertEquals(BuildConfig.DEEPSEEK_VISION_MODEL, ds.visionModel);
        assertFalse(ds.enabled);
        ModelServiceConfig qw = LlmPresets.preset("dashscope");
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", qw.baseUrl);
        assertEquals("qwen-plus", qw.chatModel);
        assertEquals(BuildConfig.QWEN_VL_MODEL, qw.visionModel);
        ModelServiceConfig custom = LlmPresets.preset("whatever");
        assertEquals("custom", custom.provider);
        assertEquals("", custom.baseUrl);
        assertEquals("llm.example.com", LlmPresets.displayName("custom", " ", "https://llm.example.com/v1", true));
    }
}
