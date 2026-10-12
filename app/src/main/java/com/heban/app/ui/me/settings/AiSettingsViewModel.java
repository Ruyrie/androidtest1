package com.heban.app.ui.me.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.heban.app.BuildConfig;
import com.heban.app.R;
import com.heban.app.data.model.ModelServiceConfig;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.repository.AiRepository;
import com.heban.app.data.repository.ModelServiceRepository;
import com.heban.app.di.AppContainer;
import com.heban.app.ui.common.BaseViewModel;
import com.heban.app.ui.common.SingleLiveEvent;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * M11 AI 助手设置：回答风格、回答时参考、对话记录（清空可撤销：Snackbar 消失后才真正删除，离开页面立即执行），
 * 以及「模型服务」（技术准备文档 9.6 表 9-5）。
 */
public class AiSettingsViewModel extends BaseViewModel {

    /** 「测试连接」结果 */
    public static final class TestResult {
        public final boolean success;
        /** 成功时为模型名，失败时为错误提示 */
        @NonNull public final String text;

        TestResult(boolean success, @NonNull String text) {
            this.success = success;
            this.text = text;
        }
    }

    private final AiRepository ai;
    private final ModelServiceRepository models;
    private final AiSettingsPrefs prefs;
    private boolean started;
    private boolean formLoaded;

    // 回答风格与参考
    public final MutableLiveData<String> style = new MutableLiveData<>(AiSettingsPrefs.STYLE_DETAIL);
    public final MutableLiveData<Boolean> useCrops = new MutableLiveData<>(true);
    public final MutableLiveData<Boolean> useWeather = new MutableLiveData<>(true);
    public final MutableLiveData<Boolean> saveHistory = new MutableLiveData<>(true);

    // 对话记录
    /** {会话数, 占用字节}；null 为加载中 */
    public final MutableLiveData<long[]> historyStats = new MutableLiveData<>(null);
    /** 已点「清空全部对话」，等待 Snackbar 消失 */
    public final MutableLiveData<Boolean> clearPending = new MutableLiveData<>(false);
    /** 显示「已清空 N 个对话 · 撤销」 */
    public final SingleLiveEvent<Integer> showUndo = new SingleLiveEvent<>();

    // 模型服务
    public final boolean hasBuiltInKey;
    /** 用户配置已启用（「当前使用：我的配置」） */
    public final MutableLiveData<Boolean> userConfigActive = new MutableLiveData<>(false);
    /** 当前生效的服务商与问答模型（状态行副标题） */
    public final MutableLiveData<String> activeProvider = new MutableLiveData<>(ModelServiceConfig.PROVIDER_DEEPSEEK);
    public final MutableLiveData<String> activeModel = new MutableLiveData<>(BuildConfig.DEEPSEEK_CHAT_MODEL);
    public final MutableLiveData<String> provider = new MutableLiveData<>(ModelServiceConfig.PROVIDER_DEEPSEEK);
    public final MutableLiveData<String> baseUrl = new MutableLiveData<>("");
    public final MutableLiveData<String> apiKey = new MutableLiveData<>("");
    public final MutableLiveData<String> chatModel = new MutableLiveData<>("");
    public final MutableLiveData<String> visionModel = new MutableLiveData<>("");
    /** 已保存 Key 的后 4 位（没有为 null） */
    public final MutableLiveData<String> savedKeyLast4 = new MutableLiveData<>(null);
    public final MutableLiveData<Integer> baseUrlError = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> apiKeyError = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> chatModelError = new MutableLiveData<>(0);
    public final MutableLiveData<Boolean> testing = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    public final MutableLiveData<TestResult> testResult = new MutableLiveData<>(null);
    public final SingleLiveEvent<Integer> toastRes = new SingleLiveEvent<>();

    public AiSettingsViewModel(AppContainer c) {
        this(c, c.aiRepository, c.modelServiceRepository, AiSettingsPrefs.of(c.settings));
    }

    AiSettingsViewModel(@Nullable AppContainer c, @NonNull AiRepository ai, @NonNull ModelServiceRepository models,
                        @NonNull AiSettingsPrefs prefs) {
        super(c);
        this.ai = ai;
        this.models = models;
        this.prefs = prefs;
        this.hasBuiltInKey = models.hasBuiltInKey();
        applyPreset(ModelServiceConfig.PROVIDER_DEEPSEEK);
    }

    public void start() {
        if (started) return;
        started = true;
        add(prefs.observeStyle().observeOn(AndroidSchedulers.mainThread())
                .subscribe(style::setValue, e -> { }));
        observeFlag(AiSettingsPrefs.Flag.USE_CROPS, useCrops);
        observeFlag(AiSettingsPrefs.Flag.USE_WEATHER, useWeather);
        observeFlag(AiSettingsPrefs.Flag.SAVE_HISTORY, saveHistory);
        add(models.observeUserConfig().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUserConfig, e -> toast.setValue(AppError.from(e).message)));
        refreshStats();
    }

    private void observeFlag(AiSettingsPrefs.Flag flag, MutableLiveData<Boolean> target) {
        add(prefs.observeFlag(flag).observeOn(AndroidSchedulers.mainThread())
                .subscribe(target::setValue, e -> { }));
    }

    // ---------- 回答风格与参考 ----------

    public void setStyle(@NonNull String value) {
        if (value.equals(style.getValue())) return;
        style.setValue(value);
        add(prefs.setStyle(value).subscribe(() -> { }, e -> { }));
    }

    public void setFlag(@NonNull AiSettingsPrefs.Flag flag, boolean value) {
        MutableLiveData<Boolean> target = flag == AiSettingsPrefs.Flag.USE_CROPS ? useCrops
                : flag == AiSettingsPrefs.Flag.USE_WEATHER ? useWeather : saveHistory;
        if (Boolean.valueOf(value).equals(target.getValue())) return;
        target.setValue(value);
        add(prefs.setFlag(flag, value).subscribe(() -> { }, e -> { }));
    }

    // ---------- 对话记录 ----------

    /** 返回本页时刷新（D06 中可能新增了对话） */
    public void refreshStats() {
        add(ai.historyStats().observeOn(AndroidSchedulers.mainThread())
                .subscribe(historyStats::setValue, e -> historyStats.setValue(new long[]{0, 0})));
    }

    /** 「清空全部对话」：先显示可撤销的 Snackbar，消失后才执行 */
    public void requestClear() {
        if (Boolean.TRUE.equals(clearPending.getValue())) return;
        long[] s = historyStats.getValue();
        int count = s == null ? 0 : (int) s[0];
        if (count <= 0) {
            toastRes.setValue(R.string.ai_settings_history_none);
            return;
        }
        clearPending.setValue(true);
        showUndo.setValue(count);
    }

    public void undoClear() {
        clearPending.setValue(false);
    }

    /** Snackbar 消失（未撤销）或离开页面时执行；只执行一次 */
    public void commitClear() {
        if (!Boolean.TRUE.equals(clearPending.getValue())) return;
        clearPending.setValue(false);
        historyStats.setValue(new long[]{0, 0});
        // 不随页面取消：离开页面时也要完成
        //noinspection ResultOfMethodCallIgnored
        ai.clearAll().observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::refreshStats, e -> {
                    toast.setValue(AppError.from(e).message);
                    refreshStats();
                });
    }

    // ---------- 模型服务 ----------

    private void onUserConfig(@Nullable ModelServiceConfig cfg) {
        boolean enabled = cfg != null && cfg.enabled;
        userConfigActive.setValue(enabled);
        savedKeyLast4.setValue(enabled && cfg.apiKeyLast4 != null && !cfg.apiKeyLast4.isEmpty()
                ? cfg.apiKeyLast4 : null);
        if (enabled) {
            activeProvider.setValue(ModelServicePresets.normalize(cfg.provider));
            activeModel.setValue(cfg.chatModel == null ? "" : cfg.chatModel);
        } else {
            activeProvider.setValue(ModelServiceConfig.PROVIDER_DEEPSEEK);
            activeModel.setValue(BuildConfig.DEEPSEEK_CHAT_MODEL);
        }
        if (formLoaded) return;
        formLoaded = true;
        if (enabled) {
            provider.setValue(ModelServicePresets.normalize(cfg.provider));
            baseUrl.setValue(cfg.baseUrl == null ? "" : cfg.baseUrl);
            chatModel.setValue(cfg.chatModel == null ? "" : cfg.chatModel);
            visionModel.setValue(cfg.visionModel == null ? "" : cfg.visionModel);
        } else {
            applyPreset(ModelServiceConfig.PROVIDER_DEEPSEEK);
        }
        apiKey.setValue("");
    }

    /** 选择服务商：DeepSeek / 通义千问填入预设的 Base URL 和模型名，自定义保留当前内容 */
    public void selectProvider(@NonNull String value) {
        String p = ModelServicePresets.normalize(value);
        if (p.equals(provider.getValue())) return;
        applyPreset(p);
        clearErrors();
        testResult.setValue(null);
    }

    private void applyPreset(String p) {
        provider.setValue(p);
        if (!ModelServicePresets.isPreset(p)) return;
        baseUrl.setValue(ModelServicePresets.baseUrl(p));
        chatModel.setValue(ModelServicePresets.chatModel(p));
        visionModel.setValue(ModelServicePresets.visionModel(p));
    }

    public boolean isBaseUrlEditable() {
        return !ModelServicePresets.isPreset(provider.getValue());
    }

    private void clearErrors() {
        baseUrlError.setValue(0);
        apiKeyError.setValue(0);
        chatModelError.setValue(0);
    }

    /** 校验表单并显示错误；通过时返回配置 */
    @Nullable
    private ModelServiceConfig validForm() {
        ModelServiceForm.Errors err = ModelServiceForm.validate(baseUrl.getValue(), apiKey.getValue(),
                chatModel.getValue(), savedKeyLast4.getValue() != null);
        baseUrlError.setValue(err.baseUrl);
        apiKeyError.setValue(err.apiKey);
        chatModelError.setValue(err.chatModel);
        if (!err.ok()) return null;
        ModelServiceConfig cfg = ModelServiceForm.build(provider.getValue(), baseUrl.getValue(), apiKey.getValue(),
                chatModel.getValue(), visionModel.getValue());
        baseUrl.setValue(cfg.baseUrl);
        return cfg;
    }

    /** 「测试连接」：发送一条「你好」，显示成功（模型名）或失败原因 */
    public void testConnection() {
        if (Boolean.TRUE.equals(testing.getValue())) return;
        ModelServiceConfig cfg = validForm();
        if (cfg == null) return;
        testing.setValue(true);
        testResult.setValue(null);
        add(models.test(cfg).observeOn(AndroidSchedulers.mainThread())
                .subscribe(model -> {
                    testing.setValue(false);
                    testResult.setValue(new TestResult(true,
                            model == null || model.isEmpty() ? cfg.chatModel : model));
                }, e -> {
                    testing.setValue(false);
                    testResult.setValue(new TestResult(false, AppError.from(e).message));
                }));
    }

    /** 「保存」：保存并启用用户配置；Key 留空时保留原来的 Key */
    public void save() {
        if (Boolean.TRUE.equals(saving.getValue())) return;
        ModelServiceConfig cfg = validForm();
        if (cfg == null) return;
        saving.setValue(true);
        add(models.save(cfg).observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    saving.setValue(false);
                    apiKey.setValue("");
                    toastRes.setValue(R.string.ai_settings_saved);
                }, e -> {
                    saving.setValue(false);
                    toast.setValue(AppError.from(e).message);
                }));
    }

    /** 「使用内置配置」：清除用户配置，表单恢复为 DeepSeek 预设 */
    public void useBuiltIn() {
        if (!Boolean.TRUE.equals(userConfigActive.getValue())) return;
        add(models.clear().observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    applyPreset(ModelServiceConfig.PROVIDER_DEEPSEEK);
                    apiKey.setValue("");
                    clearErrors();
                    testResult.setValue(null);
                    toastRes.setValue(R.string.ai_settings_restored);
                }, e -> toast.setValue(AppError.from(e).message)));
    }

    @Override
    protected void onCleared() {
        commitClear();
        super.onCleared();
    }
}
