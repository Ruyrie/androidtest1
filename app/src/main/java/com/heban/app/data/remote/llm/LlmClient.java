package com.heban.app.data.remote.llm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.heban.app.data.remote.NetworkModule;
import com.heban.app.data.remote.llm.dto.ChatChunk;
import com.heban.app.data.remote.llm.dto.ChatRequest;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/**
 * OpenAI 兼容 Chat Completions 客户端（技术准备文档 9.3）：stream() 把 SSE 转成 Flowable&lt;String&gt;
 * （逐段发射 delta.content，收到 [DONE] 或连接正常关闭时完成，取消订阅时断开连接）；
 * complete() 为非流式请求（识图、测试连接）。HTTP 错误统一转为 {@link LlmException}。
 * 请求 DeepSeek 时显式关闭默认开启的思考模式；reasoning_content 不解析。
 */
public final class LlmClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient sseClient;
    private final LlmApi api;
    private final Gson gson;

    public LlmClient(OkHttpClient base, Gson gson) {
        this.gson = gson;
        this.sseClient = base.newBuilder().readTimeout(60, TimeUnit.SECONDS).build();
        this.api = NetworkModule.retrofit("https://api.deepseek.com/", base, gson).create(LlmApi.class);
    }

    /** 流式请求：发射回答片段 */
    public Flowable<String> stream(LlmConfig cfg, ChatRequest req) {
        prepare(cfg, req, true);
        return Flowable.create(emitter -> {
            Request request = new Request.Builder()
                    .url(cfg.chatCompletionsUrl())
                    .header("Authorization", "Bearer " + cfg.apiKey)
                    .header("Accept", "text/event-stream")
                    .post(RequestBody.create(gson.toJson(req), JSON))
                    .build();
            EventSource source = EventSources.createFactory(sseClient)
                    .newEventSource(request, new Listener(emitter));
            emitter.setCancellable(source::cancel);     // 取消订阅时断开连接
        }, BackpressureStrategy.BUFFER);
    }

    /** 非流式请求：返回回答全文（choices[0].message.content） */
    public Single<String> complete(LlmConfig cfg, ChatRequest req) {
        return completeResponse(cfg, req).map(LlmClient::contentOf);
    }

    /** 非流式请求：返回完整响应（测试连接时读取 model） */
    public Single<ChatChunk> completeResponse(LlmConfig cfg, ChatRequest req) {
        return Single.defer(() -> {
            prepare(cfg, req, false);
            return api.complete(cfg.chatCompletionsUrl(), "Bearer " + cfg.apiKey, req);
        }).onErrorResumeNext(e -> Single.error(LlmException.fromRetrofit(e)));
    }

    /** 取出非流式响应的回答；没有内容时视为返回格式错误 */
    @NonNull
    static String contentOf(@Nullable ChatChunk resp) throws LlmException {
        if (resp != null && resp.error != null && (resp.choices == null || resp.choices.isEmpty())) {
            throw LlmException.fromErrorBody(resp.error);
        }
        if (resp == null || resp.choices == null || resp.choices.isEmpty()
                || resp.choices.get(0).message == null || resp.choices.get(0).message.content == null) {
            throw new JsonParseException("模型没有返回内容");
        }
        return resp.choices.get(0).message.content;
    }

    private static void prepare(LlmConfig cfg, ChatRequest req, boolean stream) {
        req.stream = stream;
        if (req.model == null || req.model.isEmpty()) req.model = cfg.model;
        if (cfg.isDeepSeekHost()) req.thinking = ChatRequest.Thinking.disabled();
    }

    private final class Listener extends EventSourceListener {
        private final FlowableEmitter<String> emitter;

        Listener(FlowableEmitter<String> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onEvent(@NonNull EventSource es, @Nullable String id, @Nullable String type,
                            @NonNull String data) {
            String d = data.trim();
            if ("[DONE]".equals(d)) {
                emitter.onComplete();
                return;
            }
            if (d.isEmpty()) return;
            ChatChunk chunk;
            try {
                chunk = gson.fromJson(d, ChatChunk.class);
            } catch (JsonParseException e) {
                emitter.tryOnError(e);
                return;
            }
            if (chunk == null) return;
            if (chunk.error != null) {
                emitter.tryOnError(LlmException.fromErrorBody(chunk.error));
                return;
            }
            if (chunk.choices == null || chunk.choices.isEmpty()) return;
            ChatChunk.Delta delta = chunk.choices.get(0).delta;
            if (delta != null && delta.content != null && !delta.content.isEmpty()) {
                emitter.onNext(delta.content);
            }
        }

        @Override
        public void onClosed(@NonNull EventSource es) {
            emitter.onComplete();
        }

        @Override
        public void onFailure(@NonNull EventSource es, @Nullable Throwable t, @Nullable Response r) {
            emitter.tryOnError(LlmException.from(t, r));   // 401 / 402 / 429 等
        }
    }
}
