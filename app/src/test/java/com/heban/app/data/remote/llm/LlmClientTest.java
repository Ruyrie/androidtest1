package com.heban.app.data.remote.llm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.remote.llm.dto.ChatMessage;
import com.heban.app.data.remote.llm.dto.ChatRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.subscribers.TestSubscriber;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class LlmClientTest {
    private MockWebServer server;
    private LlmClient client;
    private LlmConfig cfg;
    private final Gson gson = new Gson();

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new LlmClient(new OkHttpClient(), gson);
        cfg = LlmConfig.custom(server.url("/v1/").toString(), "test-key", "test-model");
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private static ChatRequest request(String question) {
        ChatRequest req = AiPrompts.chatRequest("test-model", "sys", java.util.Collections.emptyList(), question);
        req.stream = false;   // stream() 会改为 true
        return req;
    }

    private static MockResponse sse(String body) {
        return new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(body);
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }

    @Test
    public void stream_emitsChunksUntilDone() throws Exception {
        server.enqueue(sse("data: {\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"\"}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"缺镁\"}}]}\n\n"
                + ": keep-alive\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"的表现\"}}]}\n\n"
                + "data: {\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"total_tokens\":512}}\n\n"
                + "data: [DONE]\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"不应出现\"}}]}\n\n"));

        client.stream(cfg, request("番茄叶片发黄"))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValues("缺镁", "的表现")
                .assertComplete();

        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("/v1/chat/completions", req.getPath());
        assertEquals("Bearer test-key", req.getHeader("Authorization"));
        assertEquals("text/event-stream", req.getHeader("Accept"));
        JsonObject body = gson.fromJson(req.getBody().readUtf8(), JsonObject.class);
        assertTrue(body.get("stream").getAsBoolean());
        assertEquals("test-model", body.get("model").getAsString());
        assertEquals(1024, body.get("max_tokens").getAsInt());
        assertEquals(0.7, body.get("temperature").getAsDouble(), 1e-9);
        assertFalse("非 DeepSeek 服务不发送 thinking", body.has("thinking"));
        assertEquals("system", body.getAsJsonArray("messages").get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("番茄叶片发黄", body.getAsJsonArray("messages").get(1).getAsJsonObject().get("content").getAsString());
    }

    @Test
    public void stream_ignoresReasoningContent() {
        server.enqueue(sse("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"思考中\"}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"答案\",\"reasoning_content\":null}}]}\n\n"
                + "data: [DONE]\n\n"));
        client.stream(cfg, request("q"))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValues("答案")
                .assertComplete();
    }

    @Test
    public void stream_completesWhenServerClosesWithoutDone() {
        server.enqueue(sse("data: {\"choices\":[{\"delta\":{\"content\":\"一段\"}}]}\n\n"));
        client.stream(cfg, request("q"))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValues("一段")
                .assertComplete();
    }

    @Test
    public void stream_401_isLlmExceptionWithKeyHint() {
        server.enqueue(json(401, "{\"error\":{\"message\":\"Authentication Fails\",\"type\":\"authentication_error\"}}"));
        TestSubscriber<String> ts = client.stream(cfg, request("q")).test().awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoValues();
        Throwable e = errorOf(ts);
        assertTrue(e instanceof LlmException);
        assertEquals(401, ((LlmException) e).code);
        assertEquals("Authentication Fails", ((LlmException) e).serverMessage);
        AppError err = AppError.from(e);
        assertEquals(AppError.Type.UNAUTHORIZED, err.type);
        assertEquals("API Key 无效，请到「AI 助手设置 → 模型服务」检查", err.message);
    }

    @Test
    public void stream_402_and_429_and_503_mapToAppErrors() {
        server.enqueue(json(402, "{\"error\":{\"message\":\"Insufficient Balance\"}}"));
        server.enqueue(json(429, "rate limited"));
        server.enqueue(json(503, ""));
        assertEquals("模型服务余额不足", firstError().message);
        assertEquals("操作太频繁，请稍后再试", firstError().message);
        assertEquals("服务暂时不可用，请稍后重试", firstError().message);
    }

    private AppError firstError() {
        TestSubscriber<String> ts = client.stream(cfg, request("q")).test().awaitDone(5, TimeUnit.SECONDS);
        return AppError.from(errorOf(ts));
    }

    @Test
    public void stream_errorChunk_fails() {
        server.enqueue(sse("data: {\"choices\":[{\"delta\":{\"content\":\"前半\"}}]}\n\n"
                + "data: {\"error\":{\"message\":\"overloaded\",\"code\":503}}\n\n"));
        TestSubscriber<String> ts = client.stream(cfg, request("q")).test().awaitDone(5, TimeUnit.SECONDS);
        ts.assertValues("前半");
        Throwable e = errorOf(ts);
        assertTrue(e instanceof LlmException);
        assertEquals(503, ((LlmException) e).code);
    }

    @Test
    public void stream_networkFailure_isNetworkError() throws Exception {
        MockWebServer closed = new MockWebServer();
        closed.start();
        LlmConfig down = LlmConfig.custom(closed.url("/").toString(), "k", "m");
        closed.shutdown();
        TestSubscriber<String> ts = client.stream(down, request("q")).test().awaitDone(10, TimeUnit.SECONDS);
        AppError err = AppError.from(errorOf(ts));
        assertEquals(AppError.Type.NETWORK, err.type);
    }

    @Test
    public void stream_cancelOnDispose() throws Exception {
        server.enqueue(sse("data: {\"choices\":[{\"delta\":{\"content\":\"a\"}}]}\n\n")
                .setBodyDelay(3, TimeUnit.SECONDS));
        TestSubscriber<String> ts = client.stream(cfg, request("q")).test();
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS));
        ts.cancel();
        Thread.sleep(200);
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void deepSeekHost_disablesThinking() {
        ChatRequest req = request("q");
        LlmConfig ds = new LlmConfig("deepseek", "DeepSeek", "https://api.deepseek.com/", "k", "deepseek-v4-flash");
        client.stream(ds, req);   // 不订阅：只检查请求体的准备
        assertNotNull(req.thinking);
        assertEquals("disabled", req.thinking.type);
        assertTrue(req.stream);
        assertNull(gson.fromJson(gson.toJson(request("q")), JsonObject.class).get("thinking"));
    }

    @Test
    public void complete_returnsMessageContent_nonStream() throws Exception {
        server.enqueue(json(200, "{\"model\":\"test-model\",\"choices\":[{\"index\":0,"
                + "\"message\":{\"role\":\"assistant\",\"content\":\"你好！\"},\"finish_reason\":\"stop\"}]}"));
        ChatRequest req = request("你好");
        req.stream = true;
        assertEquals("你好！", client.complete(cfg, req).blockingGet());
        RecordedRequest r = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("/v1/chat/completions", r.getPath());
        assertEquals("Bearer test-key", r.getHeader("Authorization"));
        JsonObject body = gson.fromJson(r.getBody().readUtf8(), JsonObject.class);
        assertFalse(body.get("stream").getAsBoolean());
    }

    @Test
    public void complete_httpError_isLlmException() {
        server.enqueue(json(500, "{\"error\":{\"message\":\"Server Error\"}}"));
        try {
            client.complete(cfg, request("q")).blockingGet();
            throw new AssertionError("应当失败");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            assertTrue(cause instanceof LlmException);
            assertEquals(500, ((LlmException) cause).code);
            assertEquals("Server Error", ((LlmException) cause).serverMessage);
        }
    }

    @Test
    public void complete_emptyChoices_isParseError() {
        server.enqueue(json(200, "{\"choices\":[]}"));
        Throwable e = errorOf(client.complete(cfg, request("q")).test().awaitDone(5, TimeUnit.SECONDS));
        assertTrue(e instanceof JsonParseException);
        assertEquals(AppError.Type.PARSE, AppError.from(e).type);
    }

    @Test
    public void imageMessage_serializesContentParts() {
        ChatRequest req = VisionPrompts.build("vl", "data:image/jpeg;base64,AAAA", null, "黄瓜");
        JsonObject body = gson.fromJson(gson.toJson(req), JsonObject.class);
        JsonObject user = body.getAsJsonArray("messages").get(1).getAsJsonObject();
        assertEquals("image_url", user.getAsJsonArray("content").get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("data:image/jpeg;base64,AAAA", user.getAsJsonArray("content").get(0).getAsJsonObject()
                .getAsJsonObject("image_url").get("url").getAsString());
        assertEquals("识别模式：病虫害；用户选择的作物：黄瓜",
                user.getAsJsonArray("content").get(1).getAsJsonObject().get("text").getAsString());
        assertEquals(800, body.get("max_tokens").getAsInt());
        assertEquals(0.2, body.get("temperature").getAsDouble(), 1e-9);
        assertEquals("识别模式：病虫害；用户选择的作物：黄瓜", ((ChatMessage) req.messages.get(1)).textContent());
    }

    @Test
    public void errorMessage_parsing() {
        assertEquals("bad key", LlmException.parseMessage("{\"error\":{\"message\":\"bad key\"}}"));
        assertEquals("plain text", LlmException.parseMessage("plain text"));
        assertNull(LlmException.parseMessage("  "));
    }

    /** 取出 TestObserver / TestSubscriber 的错误（RxJava 3 没有公开的 errors()） */
    static Throwable errorOf(io.reactivex.rxjava3.observers.BaseTestConsumer<?, ?> t) {
        Throwable[] holder = new Throwable[1];
        t.assertError(e -> {
            holder[0] = e;
            return true;
        });
        return holder[0];
    }
}
