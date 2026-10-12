package com.heban.app.data.remote.llm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.heban.app.data.model.ScanMode;
import com.heban.app.data.remote.AppError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.observers.TestObserver;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** 主模型失败时按表 9-4 切换备用模型（MockWebServer 模拟两个服务） */
public class VisionRecognizerTest {
    private static final String RESULT = "{\"image_ok\":true,\"crop\":\"黄瓜\",\"top\":{\"name\":\"黄瓜霜霉病\","
            + "\"category\":\"卵菌病害\",\"part\":\"叶部\",\"confidence\":0.92,\"severity\":\"中度\","
            + "\"evidence\":\"叶背有灰黑色霉层\",\"advice\":[\"上午通风排湿\"],\"urgency_days\":3},"
            + "\"others\":[{\"name\":\"缺镁症\",\"confidence\":0.02}]}";

    private final Gson gson = new Gson();
    private MockWebServer server;
    private final Map<String, MockResponse> responses = new ConcurrentHashMap<>();
    private final AtomicInteger primaryCalls = new AtomicInteger();
    private final AtomicInteger backupCalls = new AtomicInteger();
    private final Map<String, String> bodies = new ConcurrentHashMap<>();
    private VisionRecognizer recognizer;
    private LlmConfig primary;
    private LlmConfig backup;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @NonNull
            @Override
            public MockResponse dispatch(@NonNull RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                String key = path.startsWith("/primary") ? "primary" : "backup";
                (key.equals("primary") ? primaryCalls : backupCalls).incrementAndGet();
                bodies.put(key, request.getBody().readUtf8());
                MockResponse r = responses.get(key);
                return r != null ? r : new MockResponse().setResponseCode(404);
            }
        });
        server.start();
        recognizer = new VisionRecognizer(new LlmClient(new OkHttpClient(), gson), gson, 1000);
        primary = new LlmConfig("deepseek", "DeepSeek", server.url("/primary").toString(), "pk", "vision-exp");
        backup = new LlmConfig("dashscope", "通义千问 VL", server.url("/backup/").toString(), "bk", "qwen-vl-plus");
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private static MockResponse ok(String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "assistant");
        msg.addProperty("content", content);
        JsonObject choice = new JsonObject();
        choice.add("message", msg);
        com.google.gson.JsonArray choices = new com.google.gson.JsonArray();
        choices.add(choice);
        JsonObject body = new JsonObject();
        body.add("choices", choices);
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body.toString());
    }

    private static MockResponse status(int code) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"HTTP " + code + "\"}}");
    }

    private TestObserver<VisionRecognizer.Recognition> run(LlmConfig b) {
        return recognizer.recognize(primary, b, "data:image/jpeg;base64,AAAA", ScanMode.PEST, "黄瓜")
                .test()
                .awaitDone(10, TimeUnit.SECONDS);
    }

    @Test
    public void primarySuccess_backupNotCalled() {
        responses.put("primary", ok(RESULT));
        responses.put("backup", ok(RESULT));
        VisionRecognizer.Recognition r = run(backup).assertComplete().values().get(0);
        assertEquals("DeepSeek", r.provider);
        assertEquals("黄瓜霜霉病", r.result.top.name);
        assertEquals(1, primaryCalls.get());
        assertEquals(0, backupCalls.get());
        JsonObject body = gson.fromJson(bodies.get("primary"), JsonObject.class);
        assertEquals("vision-exp", body.get("model").getAsString());
        assertEquals(false, body.get("stream").getAsBoolean());
        assertEquals(800, body.get("max_tokens").getAsInt());
        assertEquals(VisionPrompts.SYSTEM,
                body.getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString());
    }

    @Test
    public void primary500_switchesToBackup() {
        responses.put("primary", status(500));
        responses.put("backup", ok("```json\n" + RESULT + "\n```"));
        VisionRecognizer.Recognition r = run(backup).assertComplete().values().get(0);
        assertEquals("通义千问 VL", r.provider);
        assertEquals(1, primaryCalls.get());
        assertEquals(1, backupCalls.get());
        JsonObject body = gson.fromJson(bodies.get("backup"), JsonObject.class);
        assertEquals("qwen-vl-plus", body.get("model").getAsString());
    }

    @Test
    public void primary404_and_400_switchToBackup() {
        responses.put("primary", status(404));
        responses.put("backup", ok(RESULT));
        assertEquals("通义千问 VL", run(backup).values().get(0).provider);
        responses.put("primary", status(400));
        assertEquals("通义千问 VL", run(backup).values().get(0).provider);
    }

    @Test
    public void primaryInvalidJson_switchesToBackup() {
        responses.put("primary", ok("抱歉，我看不清这张图片。"));
        responses.put("backup", ok(RESULT));
        assertEquals("通义千问 VL", run(backup).assertComplete().values().get(0).provider);
        responses.put("primary", ok(RESULT.replace("0.92", "92")));
        assertEquals("通义千问 VL", run(backup).assertComplete().values().get(0).provider);
    }

    @Test
    public void primaryTimeout_switchesToBackup() {
        responses.put("primary", ok(RESULT).setHeadersDelay(3, TimeUnit.SECONDS));
        responses.put("backup", ok(RESULT));
        VisionRecognizer.Recognition r = run(backup).assertComplete().values().get(0);
        assertEquals("通义千问 VL", r.provider);
    }

    @Test
    public void primary401_402_429_doNotSwitch() {
        responses.put("backup", ok(RESULT));
        responses.put("primary", status(401));
        TestObserver<VisionRecognizer.Recognition> t = run(backup);
        assertEquals(AppError.Type.UNAUTHORIZED, AppError.from(LlmClientTest.errorOf(t)).type);
        responses.put("primary", status(402));
        assertEquals("模型服务余额不足", AppError.from(LlmClientTest.errorOf(run(backup))).message);
        responses.put("primary", status(429));
        assertEquals(AppError.Type.RATE_LIMIT, AppError.from(LlmClientTest.errorOf(run(backup))).type);
        assertEquals(0, backupCalls.get());
    }

    @Test
    public void noBackup_primaryErrorPropagates() {
        responses.put("primary", status(503));
        Throwable e = LlmClientTest.errorOf(run(null));
        assertTrue(e instanceof LlmException);
        assertEquals(503, ((LlmException) e).code);
        assertEquals(0, backupCalls.get());
    }

    @Test
    public void bothFail_backupErrorReported() {
        responses.put("primary", status(500));
        responses.put("backup", status(429));
        Throwable e = LlmClientTest.errorOf(run(backup));
        assertEquals(429, ((LlmException) e).code);
    }

    @Test
    public void imageChatAnswer_usesVisionModelNonStream_withFallback() {
        responses.put("primary", status(500));
        responses.put("backup", ok("看起来是霜霉病。"));
        VisionRecognizer.Answer a = recognizer.answer(primary, backup, cfg -> AiPrompts.imageChatRequest(cfg.model,
                        "sys", java.util.Collections.emptyList(), "这是什么病？", "data:image/jpeg;base64,AAAA"))
                .test().awaitDone(10, TimeUnit.SECONDS).assertComplete().values().get(0);
        assertEquals("看起来是霜霉病。", a.text);
        assertEquals("通义千问 VL", a.provider);
        JsonObject body = gson.fromJson(bodies.get("backup"), JsonObject.class);
        assertEquals(false, body.get("stream").getAsBoolean());
        assertEquals(1024, body.get("max_tokens").getAsInt());
        assertEquals("这是什么病？", body.getAsJsonArray("messages").get(1).getAsJsonObject()
                .getAsJsonArray("content").get(1).getAsJsonObject().get("text").getAsString());
    }
}
