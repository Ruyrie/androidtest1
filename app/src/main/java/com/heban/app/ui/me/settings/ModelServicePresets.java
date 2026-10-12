package com.heban.app.ui.me.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.heban.app.BuildConfig;
import com.heban.app.R;
import com.heban.app.data.model.ModelServiceConfig;

/**
 * M11「模型服务」的服务商预设（技术准备文档表 9-1 / 9-5）：选择 DeepSeek 或通义千问时自动填入 Base URL 和推荐模型名；
 * 模型名取 BuildConfig（local.properties 可覆盖），不写死在页面中。
 */
public final class ModelServicePresets {
    public static final String[] PROVIDERS = {
            ModelServiceConfig.PROVIDER_DEEPSEEK,
            ModelServiceConfig.PROVIDER_DASHSCOPE,
            ModelServiceConfig.PROVIDER_CUSTOM
    };
    public static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com";
    public static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    /** 通义千问的推荐问答模型（表 9-1 只列出了识图模型 qwen-vl-plus） */
    public static final String DASHSCOPE_CHAT_MODEL = "qwen-plus";

    private ModelServicePresets() {
    }

    public static boolean isPreset(@Nullable String provider) {
        return ModelServiceConfig.PROVIDER_DEEPSEEK.equals(provider)
                || ModelServiceConfig.PROVIDER_DASHSCOPE.equals(provider);
    }

    @NonNull
    public static String normalize(@Nullable String provider) {
        for (String p : PROVIDERS) if (p.equals(provider)) return p;
        return ModelServiceConfig.PROVIDER_DEEPSEEK;
    }

    @Nullable
    public static String baseUrl(@Nullable String provider) {
        if (ModelServiceConfig.PROVIDER_DEEPSEEK.equals(provider)) return DEEPSEEK_BASE_URL;
        if (ModelServiceConfig.PROVIDER_DASHSCOPE.equals(provider)) return DASHSCOPE_BASE_URL;
        return null;
    }

    @Nullable
    public static String chatModel(@Nullable String provider) {
        if (ModelServiceConfig.PROVIDER_DEEPSEEK.equals(provider)) return BuildConfig.DEEPSEEK_CHAT_MODEL;
        if (ModelServiceConfig.PROVIDER_DASHSCOPE.equals(provider)) return DASHSCOPE_CHAT_MODEL;
        return null;
    }

    @Nullable
    public static String visionModel(@Nullable String provider) {
        if (ModelServiceConfig.PROVIDER_DEEPSEEK.equals(provider)) return BuildConfig.DEEPSEEK_VISION_MODEL;
        if (ModelServiceConfig.PROVIDER_DASHSCOPE.equals(provider)) return BuildConfig.QWEN_VL_MODEL;
        return null;
    }

    @StringRes
    public static int label(@Nullable String provider) {
        if (ModelServiceConfig.PROVIDER_DASHSCOPE.equals(provider)) return R.string.ai_settings_provider_dashscope;
        if (ModelServiceConfig.PROVIDER_CUSTOM.equals(provider)) return R.string.ai_settings_provider_custom;
        return R.string.ai_settings_provider_deepseek;
    }
}
