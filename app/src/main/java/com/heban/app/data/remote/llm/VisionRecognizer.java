package com.heban.app.data.remote.llm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.heban.app.data.model.ScanMode;
import com.heban.app.data.model.ScanResult;
import com.heban.app.data.remote.llm.dto.ChatRequest;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;

/**
 * 识图调用与备用模型切换（技术准备文档 9.5）：先调用主模型（30 秒超时），按表 9-4（{@link VisionErrors}）
 * 判断失败时是否改用通义千问 VL 备用模型；返回内容按 {@link ScanResultParser} 解析，解析失败也视为该模型失败。
 * 同一张图片识别失败后由用户手动重试，不自动循环。
 */
public final class VisionRecognizer {
    public static final long TIMEOUT_MS = 30_000L;

    /** 一次识别的结果与识别服务显示名 */
    public static final class Recognition {
        @NonNull public final ScanResult result;
        /** 「DeepSeek」「通义千问 VL」或自定义服务名 */
        @NonNull public final String provider;

        public Recognition(@NonNull ScanResult result, @NonNull String provider) {
            this.result = result;
            this.provider = provider;
        }
    }

    /** 带图片提问的回答与回答服务名 */
    public static final class Answer {
        @NonNull public final String text;
        @NonNull public final String provider;

        public Answer(@NonNull String text, @NonNull String provider) {
            this.text = text;
            this.provider = provider;
        }
    }

    private final LlmClient llm;
    private final Gson gson;
    private final long timeoutMs;

    public VisionRecognizer(LlmClient llm, Gson gson) {
        this(llm, gson, TIMEOUT_MS);
    }

    public VisionRecognizer(LlmClient llm, Gson gson, long timeoutMs) {
        this.llm = llm;
        this.gson = gson;
        this.timeoutMs = timeoutMs;
    }

    /** 拍照识别：主模型 → （按表 9-4）备用模型 */
    public Single<Recognition> recognize(@NonNull LlmConfig primary, @Nullable LlmConfig backup,
                                         @NonNull String dataUrl, @Nullable ScanMode mode,
                                         @Nullable String cropHint) {
        return withFallback(primary, backup, cfg -> callVision(cfg, dataUrl, mode, cropHint));
    }

    /** D06 带图片的提问：用识图模型非流式回答（提示词不要求 JSON），同样按表 9-4 切换备用模型 */
    public Single<Answer> answer(@NonNull LlmConfig primary, @Nullable LlmConfig backup,
                                 @NonNull Function<LlmConfig, ChatRequest> requestFor) {
        return withFallback(primary, backup, cfg -> llm.complete(cfg, requestFor.apply(cfg))
                .timeout(timeoutMs * 2, TimeUnit.MILLISECONDS)
                .map(text -> new Answer(text, cfg.displayName)));
    }

    private Single<Recognition> callVision(LlmConfig cfg, String dataUrl, @Nullable ScanMode mode,
                                           @Nullable String hint) {
        return llm.complete(cfg, VisionPrompts.build(cfg.model, dataUrl, mode, hint))
                .timeout(timeoutMs, TimeUnit.MILLISECONDS)
                .map(text -> new Recognition(ScanResultParser.parse(text, gson), cfg.displayName));
    }

    /** 主模型失败且 VisionErrors.shouldFallback 时改用备用模型；没有备用模型时直接返回主模型的结果 */
    public static <T> Single<T> withFallback(@NonNull LlmConfig primary, @Nullable LlmConfig backup,
                                             @NonNull Function<LlmConfig, Single<T>> call) {
        return Single.defer(() -> {
            Single<T> first = call.apply(primary);
            if (backup == null) return first;
            return first.onErrorResumeNext(e -> VisionErrors.shouldFallback(e)
                    ? call.apply(backup)
                    : Single.error(e));
        });
    }
}
