package com.heban.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.BuildConfig;
import com.heban.app.data.local.prefs.SecureStore;
import com.heban.app.data.local.prefs.SettingsStore;
import com.heban.app.data.local.prefs.TokenCipher;
import com.heban.app.data.model.ModelServiceConfig;
import com.heban.app.data.remote.BusinessException;
import com.heban.app.data.remote.llm.LlmClient;
import com.heban.app.data.remote.llm.LlmConfig;
import com.heban.app.data.remote.llm.LlmConfigProvider;
import com.heban.app.data.remote.llm.LlmPresets;
import com.heban.app.data.remote.llm.dto.ChatMessage;
import com.heban.app.data.remote.llm.dto.ChatRequest;

import java.util.Arrays;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;

/**
 * ModelServiceRepository 的本地实现（DataStore + SecureStore，技术准备文档 9.6 / 表 9-5）。
 * 选择 DeepSeek 或通义千问时使用预设 Base URL（问答 / 识图模型留空时填推荐模型）；Base URL 自动去掉结尾斜杠；
 * API Key 用 SecureStore 加密后存入 DataStore，读取时只给出后 4 位。
 * 「测试连接」发送「你好」（max_tokens 8，非流式），HTTP 错误为 LlmException（AppError 给出表 9-6 的提示）。
 */
public final class LocalModelServiceRepository implements ModelServiceRepository {
    public static final String ERROR_INVALID_BASE_URL = "INVALID_BASE_URL";
    public static final String ERROR_NO_API_KEY = "NO_API_KEY";
    public static final String ERROR_NO_CHAT_MODEL = "NO_CHAT_MODEL";

    private final OkHttpClient baseClient;
    private final Gson gson;
    private final SettingsStore settings;
    private final SecureStore secureStore;
    private final TokenCipher cipher;
    private final LlmClient client;

    public LocalModelServiceRepository(OkHttpClient baseClient, Gson gson, SettingsStore settings,
                                       SecureStore secureStore) {
        this.baseClient = baseClient;
        this.gson = gson;
        this.settings = settings;
        this.secureStore = secureStore;
        this.cipher = secureStore;
        this.client = new LlmClient(baseClient, gson);
    }

    @Override
    public Flowable<ModelServiceConfig> observeUserConfig() {
        return Flowable.combineLatest(Arrays.asList(
                        settings.observe(SettingsStore.LLM_ENABLED, false),
                        settings.observe(SettingsStore.LLM_PROVIDER, ModelServiceConfig.PROVIDER_DEEPSEEK),
                        settings.observe(SettingsStore.LLM_BASE_URL, ""),
                        settings.observe(SettingsStore.LLM_CHAT_MODEL, ""),
                        settings.observe(SettingsStore.LLM_VISION_MODEL, ""),
                        settings.observe(SettingsStore.LLM_API_KEY_ENC, "")),
                        values -> values)
                .observeOn(Schedulers.io())
                .map(v -> toConfig((Boolean) v[0], (String) v[1], (String) v[2], (String) v[3], (String) v[4],
                        LlmConfigProvider.decryptOrEmpty(cipher, (String) v[5])))
                .subscribeOn(Schedulers.io());
    }

    /** 组装读取用的配置：没有保存过时给出 DeepSeek 预设（enabled = false），apiKey 为空，只给后 4 位 */
    @NonNull
    static ModelServiceConfig toConfig(boolean enabled, @Nullable String provider, @Nullable String baseUrl,
                                       @Nullable String chatModel, @Nullable String visionModel,
                                       @Nullable String plainKey) {
        ModelServiceConfig c = LlmPresets.preset(provider);
        boolean saved = enabled || notBlank(baseUrl) || notBlank(chatModel) || notBlank(plainKey);
        c.enabled = enabled;
        if (saved) {
            if (notBlank(baseUrl)) c.baseUrl = LlmConfig.normalizeBaseUrl(baseUrl);
            if (notBlank(chatModel)) c.chatModel = chatModel.trim();
            c.visionModel = visionModel == null ? "" : visionModel.trim();
        }
        c.apiKey = null;
        c.apiKeyLast4 = last4(plainKey);
        return c;
    }

    @Override
    public Completable save(ModelServiceConfig config) {
        return Completable.defer(() -> {
            ModelServiceConfig c = normalize(config);
            String key = trim(config.apiKey);
            String enc;
            if (!key.isEmpty()) {
                enc = cipher.encrypt(key);
            } else {
                enc = settings.get(SettingsStore.LLM_API_KEY_ENC, "").blockingGet();
                if (LlmConfigProvider.decryptOrEmpty(cipher, enc).isEmpty()) throw noApiKey();
            }
            Completable vision = trim(c.visionModel).isEmpty()
                    ? settings.remove(SettingsStore.LLM_VISION_MODEL)
                    : settings.put(SettingsStore.LLM_VISION_MODEL, c.visionModel);
            return settings.put(SettingsStore.LLM_PROVIDER, c.provider)
                    .andThen(settings.put(SettingsStore.LLM_BASE_URL, c.baseUrl))
                    .andThen(settings.put(SettingsStore.LLM_CHAT_MODEL, c.chatModel))
                    .andThen(vision)
                    .andThen(settings.put(SettingsStore.LLM_API_KEY_ENC, enc))
                    .andThen(settings.put(SettingsStore.LLM_ENABLED, true));
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable clear() {
        return settings.remove(SettingsStore.LLM_ENABLED)
                .andThen(settings.remove(SettingsStore.LLM_PROVIDER))
                .andThen(settings.remove(SettingsStore.LLM_BASE_URL))
                .andThen(settings.remove(SettingsStore.LLM_CHAT_MODEL))
                .andThen(settings.remove(SettingsStore.LLM_VISION_MODEL))
                .andThen(settings.remove(SettingsStore.LLM_API_KEY_ENC))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<String> test(ModelServiceConfig config) {
        return Single.defer(() -> {
            ModelServiceConfig c = normalize(config);
            String key = trim(config.apiKey);
            if (key.isEmpty()) {
                key = LlmConfigProvider.decryptOrEmpty(cipher,
                        settings.get(SettingsStore.LLM_API_KEY_ENC, "").blockingGet());
            }
            if (key.isEmpty()) throw noApiKey();
            LlmConfig cfg = new LlmConfig(c.provider,
                    LlmPresets.displayName(c.provider, c.chatModel, c.baseUrl, false), c.baseUrl, key, c.chatModel);
            return client.completeResponse(cfg, testRequest(cfg.model))
                    .map(resp -> resp.model != null && !resp.model.trim().isEmpty() ? resp.model.trim() : cfg.model);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public boolean hasBuiltInKey() {
        return !BuildConfig.DEEPSEEK_API_KEY.isEmpty();
    }

    /** 「测试连接」：发送「你好」，max_tokens 8，非流式 */
    @NonNull
    static ChatRequest testRequest(String model) {
        ChatRequest req = new ChatRequest();
        req.model = model;
        req.stream = false;
        req.maxTokens = 8;
        req.messages.add(ChatMessage.user("你好"));
        return req;
    }

    /**
     * 校验并规范化：DeepSeek / 通义千问使用预设 Base URL，问答 / 识图模型留空时填推荐模型；
     * 自定义服务必须填写 http(s) Base URL 和问答模型（识图模型可留空）。
     */
    @NonNull
    static ModelServiceConfig normalize(@NonNull ModelServiceConfig in) {
        ModelServiceConfig c = new ModelServiceConfig();
        c.provider = LlmPresets.provider(in.provider);
        c.enabled = true;
        boolean preset = !ModelServiceConfig.PROVIDER_CUSTOM.equals(c.provider);
        c.baseUrl = preset ? LlmPresets.baseUrl(c.provider) : LlmConfig.normalizeBaseUrl(in.baseUrl);
        if (!LlmConfig.isValidBaseUrl(c.baseUrl)) {
            throw new BusinessException(ERROR_INVALID_BASE_URL, "请填写正确的 Base URL，如 https://api.deepseek.com");
        }
        c.chatModel = trim(in.chatModel);
        if (c.chatModel.isEmpty() && preset) c.chatModel = LlmPresets.chatModel(c.provider);
        if (c.chatModel.isEmpty()) throw new BusinessException(ERROR_NO_CHAT_MODEL, "请填写问答模型");
        c.visionModel = trim(in.visionModel);
        c.apiKey = in.apiKey;
        return c;
    }

    private static BusinessException noApiKey() {
        return new BusinessException(ERROR_NO_API_KEY, "请填写 API Key");
    }

    @Nullable
    static String last4(@Nullable String key) {
        String k = trim(key);
        if (k.isEmpty()) return null;
        return k.length() <= 4 ? k : k.substring(k.length() - 4);
    }

    private static boolean notBlank(@Nullable String s) {
        return s != null && !s.trim().isEmpty();
    }

    @NonNull
    private static String trim(@Nullable String s) {
        return s == null ? "" : s.trim();
    }
}
