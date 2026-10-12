package com.heban.app.ui.me.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.heban.app.R;
import com.heban.app.data.model.ModelServiceConfig;

import java.util.regex.Pattern;

/** M11「模型服务」表单的校验与组装（纯 Java，便于单元测试）。 */
public final class ModelServiceForm {
    private static final Pattern URL = Pattern.compile("^https?://[^\\s/?#]+[^\\s]*$", Pattern.CASE_INSENSITIVE);

    /** 校验结果：各字段的错误提示（字符串资源 id），0 表示没有错误 */
    public static final class Errors {
        @StringRes public final int baseUrl;
        @StringRes public final int apiKey;
        @StringRes public final int chatModel;

        Errors(int baseUrl, int apiKey, int chatModel) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.chatModel = chatModel;
        }

        public boolean ok() {
            return baseUrl == 0 && apiKey == 0 && chatModel == 0;
        }
    }

    private ModelServiceForm() {
    }

    /** 去掉首尾空白和结尾斜杠 */
    @NonNull
    public static String normalizeBaseUrl(@Nullable String url) {
        String s = url == null ? "" : url.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /**
     * @param hasSavedKey 已保存过 Key（留空时保留原来的 Key）
     */
    @NonNull
    public static Errors validate(@Nullable String baseUrl, @Nullable String apiKey, @Nullable String chatModel,
                                  boolean hasSavedKey) {
        String url = normalizeBaseUrl(baseUrl);
        int urlErr = url.isEmpty() ? R.string.ai_settings_err_base_url_empty
                : URL.matcher(url).matches() ? 0 : R.string.ai_settings_err_base_url_format;
        int keyErr = trim(apiKey).isEmpty() && !hasSavedKey ? R.string.ai_settings_err_api_key : 0;
        int modelErr = trim(chatModel).isEmpty() ? R.string.ai_settings_err_chat_model : 0;
        return new Errors(urlErr, keyErr, modelErr);
    }

    /** 组装配置：apiKey 为空表示沿用已保存的 Key；识图模型为空时为 null */
    @NonNull
    public static ModelServiceConfig build(@Nullable String provider, @Nullable String baseUrl,
                                           @Nullable String apiKey, @Nullable String chatModel,
                                           @Nullable String visionModel) {
        ModelServiceConfig c = new ModelServiceConfig();
        c.enabled = true;
        c.provider = ModelServicePresets.normalize(provider);
        c.baseUrl = normalizeBaseUrl(baseUrl);
        c.apiKey = trim(apiKey);
        c.chatModel = trim(chatModel);
        String vision = trim(visionModel);
        c.visionModel = vision.isEmpty() ? null : vision;
        return c;
    }

    private static String trim(@Nullable String s) {
        return s == null ? "" : s.trim();
    }
}
