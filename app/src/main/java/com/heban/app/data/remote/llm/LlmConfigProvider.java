package com.heban.app.data.remote.llm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.BuildConfig;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.local.prefs.TokenCipher;
import com.heban.app.data.model.ModelServiceConfig;
import com.heban.app.data.remote.BusinessException;

/**
 * 决定每次请求使用哪套模型服务配置（技术准备文档 9.1）：
 * <ol>
 *   <li>用户在「AI 助手设置 → 模型服务」中填写并启用了自己的配置 → 使用用户配置
 *       （问答用用户的问答模型；识图用户填了识图模型时作为主模型）；</li>
 *   <li>否则使用 BuildConfig 中的默认配置（DeepSeek）；</li>
 *   <li>识图的备用模型固定为通义千问 VL（配置了 DASHSCOPE_API_KEY 时启用），主模型失败时自动切换。</li>
 * </ol>
 * 用户配置留空识图模型时，识图使用默认的 DeepSeek 视觉模型（用户的服务就是 DeepSeek 时用用户的 Key）。
 * 读取设置是阻塞调用，只在 io 线程使用。
 */
public final class LlmConfigProvider {
    public static final String ERROR_NOT_CONFIGURED = "LLM_NOT_CONFIGURED";

    /** BuildConfig（local.properties）中的默认配置 */
    public static final class Defaults {
        @NonNull public final String deepseekKey;
        @NonNull public final String chatModel;
        @NonNull public final String visionModel;
        @NonNull public final String dashscopeKey;
        @NonNull public final String qwenVlModel;

        public Defaults(@Nullable String deepseekKey, @Nullable String chatModel, @Nullable String visionModel,
                        @Nullable String dashscopeKey, @Nullable String qwenVlModel) {
            this.deepseekKey = trim(deepseekKey);
            this.chatModel = trim(chatModel);
            this.visionModel = trim(visionModel);
            this.dashscopeKey = trim(dashscopeKey);
            this.qwenVlModel = trim(qwenVlModel);
        }

        public static Defaults fromBuildConfig() {
            return new Defaults(BuildConfig.DEEPSEEK_API_KEY, BuildConfig.DEEPSEEK_CHAT_MODEL,
                    BuildConfig.DEEPSEEK_VISION_MODEL, BuildConfig.DASHSCOPE_API_KEY, BuildConfig.QWEN_VL_MODEL);
        }
    }

    /** 用户保存的配置（apiKey 为解密后的明文，解密失败时为空） */
    public static final class UserSettings {
        public boolean enabled;
        public String provider;
        public String baseUrl;
        public String apiKey;
        public String chatModel;
        public String visionModel;

        /** 已启用且 Key、Base URL、问答模型都已填写 */
        public boolean usable() {
            return enabled && !trim(apiKey).isEmpty() && LlmConfig.isValidBaseUrl(baseUrl)
                    && !trim(chatModel).isEmpty();
        }
    }

    /** 用户配置来源（阻塞读取） */
    public interface Source {
        @NonNull
        UserSettings read();
    }

    /** 主模型与备用模型（备用可能为 null） */
    public static final class VisionPair {
        @NonNull public final LlmConfig primary;
        @Nullable public final LlmConfig backup;

        VisionPair(@NonNull LlmConfig primary, @Nullable LlmConfig backup) {
            this.primary = primary;
            this.backup = backup;
        }
    }

    private final Source source;
    private final Defaults defaults;

    public LlmConfigProvider(SettingsStore settings, TokenCipher cipher) {
        this(() -> readSettings(settings, cipher), Defaults.fromBuildConfig());
    }

    public LlmConfigProvider(Source source, Defaults defaults) {
        this.source = source;
        this.defaults = defaults;
    }

    /** 从 DataStore 读取用户配置，Key 用 SecureStore 解密（解密失败视为没有 Key） */
    @NonNull
    public static UserSettings readSettings(SettingsStore settings, TokenCipher cipher) {
        UserSettings u = new UserSettings();
        u.enabled = settings.get(SettingsStore.LLM_ENABLED, false).blockingGet();
        u.provider = settings.get(SettingsStore.LLM_PROVIDER, ModelServiceConfig.PROVIDER_DEEPSEEK).blockingGet();
        u.baseUrl = settings.get(SettingsStore.LLM_BASE_URL, "").blockingGet();
        u.chatModel = settings.get(SettingsStore.LLM_CHAT_MODEL, "").blockingGet();
        u.visionModel = settings.get(SettingsStore.LLM_VISION_MODEL, "").blockingGet();
        String enc = settings.get(SettingsStore.LLM_API_KEY_ENC, "").blockingGet();
        u.apiKey = decryptOrEmpty(cipher, enc);
        return u;
    }

    @NonNull
    public static String decryptOrEmpty(TokenCipher cipher, @Nullable String enc) {
        if (enc == null || enc.isEmpty()) return "";
        try {
            String plain = cipher.decrypt(enc);
            return plain == null ? "" : plain;
        } catch (Exception e) {
            return "";
        }
    }

    /** 问答使用的配置；没有任何可用 Key 时为 null */
    @Nullable
    public LlmConfig chat() {
        UserSettings u = source.read();
        if (u.usable()) {
            String provider = LlmPresets.provider(u.provider);
            String model = trim(u.chatModel);
            return new LlmConfig(provider, LlmPresets.displayName(provider, model, u.baseUrl, false),
                    u.baseUrl, trim(u.apiKey), model);
        }
        return builtInChat();
    }

    /** 识图主模型；没有可用 Key 时为 null */
    @Nullable
    public LlmConfig visionPrimary() {
        UserSettings u = source.read();
        if (u.usable()) {
            String provider = LlmPresets.provider(u.provider);
            String vm = trim(u.visionModel);
            if (!vm.isEmpty()) {
                return new LlmConfig(provider, LlmPresets.displayName(provider, vm, u.baseUrl, true),
                        u.baseUrl, trim(u.apiKey), vm);
            }
            if (LlmConfig.isDeepSeekUrl(u.baseUrl) && !defaults.visionModel.isEmpty()) {
                return new LlmConfig(ModelServiceConfig.PROVIDER_DEEPSEEK, LlmConfig.NAME_DEEPSEEK,
                        u.baseUrl, trim(u.apiKey), defaults.visionModel);
            }
        }
        return builtInVision();
    }

    /** 识图备用模型：通义千问 VL（配置了 DASHSCOPE_API_KEY 时） */
    @Nullable
    public LlmConfig visionBackup() {
        if (defaults.dashscopeKey.isEmpty() || defaults.qwenVlModel.isEmpty()) return null;
        return new LlmConfig(ModelServiceConfig.PROVIDER_DASHSCOPE, LlmConfig.NAME_QWEN_VL,
                LlmConfig.DASHSCOPE_BASE_URL, defaults.dashscopeKey, defaults.qwenVlModel);
    }

    /**
     * 识图使用的主 / 备用模型：没有主模型时由备用模型单独承担；备用与主模型相同时不重复调用。
     * 都没有时为 null。
     */
    @Nullable
    public VisionPair vision() {
        LlmConfig primary = visionPrimary();
        LlmConfig backup = visionBackup();
        if (primary == null) return backup == null ? null : new VisionPair(backup, null);
        if (primary.sameEndpoint(backup)) backup = null;
        return new VisionPair(primary, backup);
    }

    @Nullable
    private LlmConfig builtInChat() {
        if (defaults.deepseekKey.isEmpty() || defaults.chatModel.isEmpty()) return null;
        return new LlmConfig(ModelServiceConfig.PROVIDER_DEEPSEEK, LlmConfig.NAME_DEEPSEEK,
                LlmConfig.DEEPSEEK_BASE_URL, defaults.deepseekKey, defaults.chatModel);
    }

    @Nullable
    private LlmConfig builtInVision() {
        if (defaults.deepseekKey.isEmpty() || defaults.visionModel.isEmpty()) return null;
        return new LlmConfig(ModelServiceConfig.PROVIDER_DEEPSEEK, LlmConfig.NAME_DEEPSEEK,
                LlmConfig.DEEPSEEK_BASE_URL, defaults.deepseekKey, defaults.visionModel);
    }

    /** 没有任何可用 Key 时的提示（只显示错误，不给示例回答） */
    public static BusinessException notConfigured() {
        return new BusinessException(ERROR_NOT_CONFIGURED,
                "还没有可用的 AI 模型服务，请到「AI 助手设置 → 模型服务」填写 API Key");
    }

    @NonNull
    static String trim(@Nullable String s) {
        return s == null ? "" : s.trim();
    }
}
