package com.heban.app.ui.me.settings;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.heban.app.BuildConfig;
import com.heban.app.R;
import com.heban.app.data.model.ModelServiceConfig;
import com.heban.app.data.remote.AppError;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AiSettingsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AiSettingsFakes.Ai ai;
    private AiSettingsFakes.Models models;
    private AiSettingsFakes.Prefs prefs;
    private AiSettingsViewModel vm;
    private final List<Integer> undo = new ArrayList<>();
    private final List<Integer> toastRes = new ArrayList<>();
    private final List<String> toasts = new ArrayList<>();

    @BeforeClass
    public static void initMainThread() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
    }

    @Before
    public void setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
        ai = new AiSettingsFakes.Ai();
        models = new AiSettingsFakes.Models();
        prefs = new AiSettingsFakes.Prefs();
    }

    private void create() {
        vm = new AiSettingsViewModel(null, ai, models, prefs);
        vm.showUndo.observeForever(undo::add);
        vm.toastRes.observeForever(toastRes::add);
        vm.toast.observeForever(toasts::add);
        vm.start();
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    // ---------- 回答风格与参考 ----------

    @Test
    public void styleAndFlags_persist() {
        create();
        assertEquals(AiSettingsPrefs.STYLE_DETAIL, vm.style.getValue());
        vm.setStyle(AiSettingsPrefs.STYLE_PLAIN);
        assertEquals(AiSettingsPrefs.STYLE_PLAIN, prefs.style.getValue());
        vm.setFlag(AiSettingsPrefs.Flag.SAVE_HISTORY, false);
        assertFalse(prefs.flags.get(AiSettingsPrefs.Flag.SAVE_HISTORY).getValue());
        assertFalse(vm.saveHistory.getValue());
        assertTrue(vm.useCrops.getValue());
    }

    // ---------- 清空全部对话（可撤销） ----------

    @Test
    public void clear_undo_doesNotDelete() {
        create();
        assertArrayEquals(new long[]{12, 2516582}, vm.historyStats.getValue());
        vm.requestClear();
        assertEquals(1, undo.size());
        assertEquals(12, (int) undo.get(0));
        assertTrue(vm.clearPending.getValue());
        vm.requestClear();                 // 重复点击不再提示
        assertEquals(1, undo.size());
        vm.undoClear();
        vm.commitClear();                  // Snackbar 因撤销而消失后不应执行
        assertEquals(0, ai.clearCalls);
        assertFalse(vm.clearPending.getValue());
    }

    @Test
    public void clear_commitOnceAfterDismiss() {
        create();
        vm.requestClear();
        assertEquals(0, ai.clearCalls);
        vm.commitClear();
        vm.commitClear();
        assertEquals(1, ai.clearCalls);
        assertArrayEquals(new long[]{0, 0}, vm.historyStats.getValue());
    }

    @Test
    public void clear_committedWhenLeavingPage() {
        create();
        vm.requestClear();
        vm.onCleared();
        assertEquals(1, ai.clearCalls);
    }

    @Test
    public void clear_nothingToClear() {
        ai.stats = new long[]{0, 0};
        create();
        vm.requestClear();
        assertTrue(undo.isEmpty());
        assertEquals(R.string.ai_settings_history_none, (int) toastRes.get(0));
    }

    @Test
    public void historySize_format() {
        assertEquals("0 KB", AiSettingsFormat.size(0));
        assertEquals("1 KB", AiSettingsFormat.size(10));
        assertEquals("36 KB", AiSettingsFormat.size(36 * 1024));
        assertEquals("2.4 MB", AiSettingsFormat.size(2516582));
    }

    // ---------- 模型服务 ----------

    @Test
    public void defaults_builtInDeepSeekPreset() {
        create();
        assertFalse(vm.userConfigActive.getValue());
        assertEquals(ModelServiceConfig.PROVIDER_DEEPSEEK, vm.provider.getValue());
        assertEquals("https://api.deepseek.com", vm.baseUrl.getValue());
        assertEquals(BuildConfig.DEEPSEEK_CHAT_MODEL, vm.chatModel.getValue());
        assertEquals(BuildConfig.DEEPSEEK_VISION_MODEL, vm.visionModel.getValue());
        assertFalse(vm.isBaseUrlEditable());
        assertNull(vm.savedKeyLast4.getValue());
        assertTrue(vm.hasBuiltInKey);
    }

    @Test
    public void presets_fillBaseUrlAndModels() {
        create();
        vm.selectProvider(ModelServiceConfig.PROVIDER_DASHSCOPE);
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", vm.baseUrl.getValue());
        assertEquals("qwen-plus", vm.chatModel.getValue());
        assertEquals(BuildConfig.QWEN_VL_MODEL, vm.visionModel.getValue());
        vm.selectProvider(ModelServiceConfig.PROVIDER_CUSTOM);
        assertTrue(vm.isBaseUrlEditable());
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", vm.baseUrl.getValue());   // 自定义保留当前值
    }

    @Test
    public void save_validatesFields() {
        create();
        vm.selectProvider(ModelServiceConfig.PROVIDER_CUSTOM);
        vm.baseUrl.setValue("api.example.com");
        vm.chatModel.setValue(" ");
        vm.save();
        assertEquals(R.string.ai_settings_err_base_url_format, (int) vm.baseUrlError.getValue());
        assertEquals(R.string.ai_settings_err_api_key, (int) vm.apiKeyError.getValue());
        assertEquals(R.string.ai_settings_err_chat_model, (int) vm.chatModelError.getValue());
        assertTrue(models.saved.isEmpty());
        vm.baseUrl.setValue("");
        vm.save();
        assertEquals(R.string.ai_settings_err_base_url_empty, (int) vm.baseUrlError.getValue());
    }

    @Test
    public void save_storesConfigAndShowsSavedKey() {
        create();
        vm.selectProvider(ModelServiceConfig.PROVIDER_CUSTOM);
        vm.baseUrl.setValue(" https://llm.example.com/v1// ");
        vm.apiKey.setValue(" sk-123456abcd ");
        vm.chatModel.setValue("my-chat");
        vm.visionModel.setValue("");
        vm.save();
        ModelServiceConfig c = models.saved.get(0);
        assertTrue(c.enabled);
        assertEquals(ModelServiceConfig.PROVIDER_CUSTOM, c.provider);
        assertEquals("https://llm.example.com/v1", c.baseUrl);
        assertEquals("sk-123456abcd", c.apiKey);
        assertEquals("my-chat", c.chatModel);
        assertNull(c.visionModel);
        assertEquals("", vm.apiKey.getValue());
        assertEquals("abcd", vm.savedKeyLast4.getValue());
        assertTrue(vm.userConfigActive.getValue());
        assertEquals("my-chat", vm.activeModel.getValue());
        assertEquals(R.string.ai_settings_saved, (int) toastRes.get(0));
        // 再次保存时 Key 可以留空（保留原来的 Key）
        vm.chatModel.setValue("my-chat-2");
        vm.save();
        assertEquals(2, models.saved.size());
        assertEquals("", models.saved.get(1).apiKey);
        assertEquals(0, (int) vm.apiKeyError.getValue());
    }

    @Test
    public void savedUserConfig_fillsForm() {
        ModelServiceConfig c = new ModelServiceConfig();
        c.enabled = true;
        c.provider = ModelServiceConfig.PROVIDER_DASHSCOPE;
        c.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        c.chatModel = "qwen-max";
        c.apiKeyLast4 = "wxyz";
        models.config.onNext(c);
        create();
        assertEquals(ModelServiceConfig.PROVIDER_DASHSCOPE, vm.provider.getValue());
        assertEquals("qwen-max", vm.chatModel.getValue());
        assertEquals("", vm.visionModel.getValue());
        assertEquals("wxyz", vm.savedKeyLast4.getValue());
        assertTrue(vm.userConfigActive.getValue());
    }

    @Test
    public void testConnection_successAndFailure() {
        create();
        vm.apiKey.setValue("sk-test");
        vm.testConnection();
        assertTrue(vm.testResult.getValue().success);
        assertEquals(BuildConfig.DEEPSEEK_CHAT_MODEL, vm.testResult.getValue().text);
        assertFalse(vm.testing.getValue());
        assertEquals("sk-test", models.tested.get(0).apiKey);

        models.testError = new AppError.HttpCodeException(401, "unauthorized");
        vm.testConnection();
        assertFalse(vm.testResult.getValue().success);
        assertEquals("API Key 无效，请到「AI 助手设置 → 模型服务」检查", vm.testResult.getValue().text);
    }

    @Test
    public void testConnection_requiresKeyWhenNoneSaved() {
        create();
        vm.testConnection();
        assertTrue(models.tested.isEmpty());
        assertEquals(R.string.ai_settings_err_api_key, (int) vm.apiKeyError.getValue());
    }

    @Test
    public void useBuiltIn_clearsAndResetsForm() {
        create();
        vm.useBuiltIn();                     // 没有用户配置时不可用
        assertEquals(0, models.clears);
        vm.selectProvider(ModelServiceConfig.PROVIDER_DASHSCOPE);
        vm.apiKey.setValue("sk-abcd1234");
        vm.save();
        vm.useBuiltIn();
        assertEquals(1, models.clears);
        assertFalse(vm.userConfigActive.getValue());
        assertEquals(ModelServiceConfig.PROVIDER_DEEPSEEK, vm.provider.getValue());
        assertEquals("https://api.deepseek.com", vm.baseUrl.getValue());
        assertNull(vm.savedKeyLast4.getValue());
        assertEquals(R.string.ai_settings_restored, (int) toastRes.get(toastRes.size() - 1));
    }

    @Test
    public void form_normalizeBaseUrl() {
        assertEquals("https://api.deepseek.com", ModelServiceForm.normalizeBaseUrl(" https://api.deepseek.com/ "));
        assertTrue(ModelServiceForm.validate("http://192.168.1.2:8000/v1", "k", "m", false).ok());
        assertFalse(ModelServiceForm.validate("ftp://x", "k", "m", false).ok());
        assertTrue(ModelServiceForm.validate("https://a.b", "", "m", true).ok());
    }
}
