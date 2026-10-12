package com.heban.app.data.remote.llm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.BuildConfig;
import com.heban.app.data.model.ModelServiceConfig;

/**
 * M11「模型服务」的服务商预设（技术准备文档表 9-5）：选择 DeepSeek 或通义千问时自动填入 Base URL 和推荐模型名。
 * DeepSeek 的模型名取 BuildConfig（local.properties 可改），不写死在代码里。
 */
public final class LlmPresets {
    /** 通义千问（阿里云百炼）推荐的问答模型 */
    public static final String QWEN_CHAT_MODEL = "qwen-plus";

    private LlmPresets() {
    }

    /** 规范化服务商取值，未知值视为自定义 */
    @NonNull
    public static String provider(@Nullable String provider) {
        if (ModelServiceConfig.PROVIDER_DEEPSEEK.equals(provider)) return ModelServiceConfig.PROVIDER_DEEPSEEK;
        if (ModelServiceConfig.PROVIDER_DASHSCOPE.equals(provider)) return ModelServiceConfig.PROVIDER_DASHSCOPE;
        return ModelServiceConfig.PROVIDER_CUSTOM;
    }

    /** 预设 Base URL；自定义为空字符串 */
    @NonNull
    public static String baseUrl(@Nullable String provider) {
        switch (provider(provider)) {
            case ModelServiceConfig.PROVIDER_DEEPSEEK:
                return LlmConfig.DEEPSEEK_BASE_URL;
            case ModelServiceConfig.PROVIDER_DASHSCOPE:
                return LlmConfig.DASHSCOPE_BASE_URL;
            default:
                return "";
        }
    }

    @NonNull
    public static String chatModel(@Nullable String provider) {
        switch (provider(provider)) {
            case ModelServiceConfig.PROVIDER_DEEPSEEK:
                return BuildConfig.DEEPSEEK_CHAT_MODEL;
            case ModelServiceConfig.PROVIDER_DASHSCOPE:
                return QWEN_CHAT_MODEL;
            default:
                return "";
        }
    }

    @NonNull
    public static String visionModel(@Nullable String provider) {
        switch (provider(provider)) {
            case ModelServiceConfig.PROVIDER_DEEPSEEK:
                return BuildConfig.DEEPSEEK_VISION_MODEL;
            case ModelServiceConfig.PROVIDER_DASHSCOPE:
                return BuildConfig.QWEN_VL_MODEL;
            default:
                return "";
        }
    }

    /** 某个服务商的预设配置（未启用、无 Key），界面切换服务商时用来填表 */
    @NonNull
    public static ModelServiceConfig preset(@Nullable String provider) {
        ModelServiceConfig c = new ModelServiceConfig();
        c.provider = provider(provider);
        c.baseUrl = baseUrl(c.provider);
        c.chatModel = chatModel(c.provider);
        c.visionModel = visionModel(c.provider);
        return c;
    }

    /**
     * 显示名：DeepSeek →「DeepSeek」；通义千问 →「通义千问」（识图为「通义千问 VL」）；
     * 自定义服务显示模型名（模型名为空时显示 Base URL 的主机名）。
     */
    @NonNull
    public static String displayName(@Nullable String provider, @Nullable String model, @Nullable String baseUrl,
                                     boolean vision) {
        switch (provider(provider)) {
            case ModelServiceConfig.PROVIDER_DEEPSEEK:
                return LlmConfig.NAME_DEEPSEEK;
            case ModelServiceConfig.PROVIDER_DASHSCOPE:
                return vision ? LlmConfig.NAME_QWEN_VL : LlmConfig.NAME_QWEN;
            default:
                if (model != null && !model.trim().isEmpty()) return model.trim();
                return host(baseUrl);
        }
    }

    @NonNull
    static String host(@Nullable String baseUrl) {
        String u = LlmConfig.normalizeBaseUrl(baseUrl);
        int scheme = u.indexOf("://");
        if (scheme >= 0) u = u.substring(scheme + 3);
        int slash = u.indexOf('/');
        if (slash >= 0) u = u.substring(0, slash);
        return u;
    }
}
