package com.heban.app.data.remote.llm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.model.ModelServiceConfig;

import java.util.Locale;
import java.util.Objects;

/** 一次请求使用的模型服务（技术准备文档 9.1）：服务商、显示名、Base URL、Key 与模型名。 */
public final class LlmConfig {
    public static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com";
    public static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    public static final String NAME_DEEPSEEK = "DeepSeek";
    public static final String NAME_QWEN = "通义千问";
    public static final String NAME_QWEN_VL = "通义千问 VL";

    /** "deepseek" / "dashscope" / "custom"（取值同 ModelServiceConfig.PROVIDER_*） */
    @NonNull public final String provider;
    /** 结果页显示「由 DeepSeek 识别」 */
    @NonNull public final String displayName;
    /** 不带结尾斜杠 */
    @NonNull public final String baseUrl;
    @NonNull public final String apiKey;
    @NonNull public final String model;

    public LlmConfig(@NonNull String provider, @NonNull String displayName, @NonNull String baseUrl,
                     @NonNull String apiKey, @NonNull String model) {
        this.provider = provider;
        this.displayName = displayName;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey.trim();
        this.model = model.trim();
    }

    /** 任意 OpenAI 兼容服务（测试与「自定义」使用），显示名为模型名 */
    public static LlmConfig custom(String baseUrl, String apiKey, String model) {
        return new LlmConfig(ModelServiceConfig.PROVIDER_CUSTOM, model, baseUrl, apiKey, model);
    }

    public String chatCompletionsUrl() {
        return baseUrl + "/chat/completions";
    }

    /** 是否为 DeepSeek 官方服务（需要显式关闭默认开启的思考模式） */
    public boolean isDeepSeekHost() {
        return isDeepSeekUrl(baseUrl);
    }

    public static boolean isDeepSeekUrl(@Nullable String baseUrl) {
        String u = normalizeBaseUrl(baseUrl).toLowerCase(Locale.ROOT);
        return u.equals(DEEPSEEK_BASE_URL) || u.startsWith(DEEPSEEK_BASE_URL + "/");
    }

    /** 同一服务、Key 与模型（避免备用模型与主模型重复） */
    public boolean sameEndpoint(@Nullable LlmConfig o) {
        return o != null && baseUrl.equals(o.baseUrl) && apiKey.equals(o.apiKey) && model.equals(o.model);
    }

    /**
     * 规范化 Base URL：去掉首尾空白和结尾斜杠；用户误填完整接口地址（…/chat/completions）时去掉该后缀。
     */
    @NonNull
    public static String normalizeBaseUrl(@Nullable String url) {
        String u = url == null ? "" : url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        String suffix = "/chat/completions";
        if (u.toLowerCase(Locale.ROOT).endsWith(suffix)) {
            u = u.substring(0, u.length() - suffix.length());
            while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    /** 是否为可用的 http(s) 地址 */
    public static boolean isValidBaseUrl(@Nullable String url) {
        String u = normalizeBaseUrl(url).toLowerCase(Locale.ROOT);
        String rest;
        if (u.startsWith("https://")) {
            rest = u.substring(8);
        } else if (u.startsWith("http://")) {
            rest = u.substring(7);
        } else {
            return false;
        }
        return !rest.isEmpty() && !rest.startsWith("/") && !rest.contains(" ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LlmConfig)) return false;
        LlmConfig c = (LlmConfig) o;
        return provider.equals(c.provider) && displayName.equals(c.displayName) && sameEndpoint(c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, displayName, baseUrl, apiKey, model);
    }

    @NonNull
    @Override
    public String toString() {
        return "LlmConfig{" + provider + ", " + baseUrl + ", " + model + "}";
    }
}
