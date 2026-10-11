package com.heban.app.data.remote.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.Gson;
import com.heban.app.data.model.CityOption;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.remote.AppError;
import com.heban.app.data.weather.LocationResolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class QWeatherApiTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String NOW = "{\"code\":\"200\",\"updateTime\":\"2026-10-10T15:02+08:00\",\"now\":{"
            + "\"obsTime\":\"2026-10-10T15:00+08:00\",\"temp\":\"24\",\"feelsLike\":\"25\",\"icon\":\"101\","
            + "\"text\":\"多云\",\"wind360\":\"123\",\"windDir\":\"东南风\",\"windScale\":\"2\",\"windSpeed\":\"8\","
            + "\"humidity\":\"68\",\"precip\":\"0.0\",\"pressure\":\"1013\",\"vis\":\"16\",\"cloud\":\"10\","
            + "\"dew\":\"17\"}}";
    private static final String HOURLY = "{\"code\":\"200\",\"hourly\":["
            + "{\"fxTime\":\"2026-10-10T16:00+08:00\",\"temp\":\"25\",\"icon\":\"100\",\"text\":\"晴\","
            + "\"windDir\":\"东南风\",\"windScale\":\"1-2\",\"humidity\":\"60\",\"pop\":\"0\",\"precip\":\"0.0\"},"
            + "{\"fxTime\":\"2026-10-10T17:00+08:00\",\"temp\":\"24\",\"icon\":\"100\",\"text\":\"晴\","
            + "\"windDir\":\"东南风\",\"windScale\":\"3-4\",\"humidity\":\"62\",\"pop\":\"\",\"precip\":\"0.3\"}]}";
    private static final String DAILY = "{\"code\":\"200\",\"daily\":["
            + "{\"fxDate\":\"2026-10-10\",\"tempMax\":\"26\",\"tempMin\":\"18\",\"iconDay\":\"101\","
            + "\"textDay\":\"多云\",\"iconNight\":\"150\",\"textNight\":\"晴\",\"windScaleDay\":\"1-2\","
            + "\"humidity\":\"72\",\"precip\":\"0.0\"},"
            + "{\"fxDate\":\"2026-10-12\",\"tempMax\":\"15\",\"tempMin\":\"8\",\"iconDay\":\"305\","
            + "\"textDay\":\"小雨\",\"windScaleDay\":\"3-4\",\"humidity\":\"90\",\"precip\":\"8.5\"}]}";
    private static final String CITY = "{\"code\":\"200\",\"location\":["
            + "{\"name\":\"江宁\",\"id\":\"101190104\",\"lat\":\"31.95\",\"lon\":\"118.85\",\"adm2\":\"南京\","
            + "\"adm1\":\"江苏省\",\"country\":\"中国\",\"type\":\"city\"},"
            + "{\"name\":\"南京\",\"id\":\"101190101\",\"adm2\":\"南京\",\"adm1\":\"江苏省\"}]}";

    private MockWebServer server;
    private QWeatherApi api;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String host = "http://" + server.getHostName() + ":" + server.getPort();
        api = QWeatherApis.create(new OkHttpClient(), new Gson(), host, " test-key ");
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }

    private static long at(int day, int hour) {
        return LocalDateTime.of(2026, 10, day, hour, 0).atZone(ZONE).toInstant().toEpochMilli();
    }

    @Test
    public void now_sendsKeyHeaderAndLocation_parsesFields() throws Exception {
        server.enqueue(json(200, NOW));
        QwNowResp r = api.now("101190104").blockingGet();
        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("test-key", req.getHeader("X-QW-Api-Key"));
        assertEquals("/v7/weather/now?location=101190104", req.getPath());
        assertEquals("200", r.code);

        WeatherBundle b = QWeatherMapper.toBundle(r, null, null, "101190104", "南京 江宁", 1L, ZONE);
        assertEquals(24, b.now.temp);
        assertEquals(25, b.now.feelsLike);
        assertEquals("多云", b.now.text);
        assertEquals("101", b.now.icon);
        assertEquals("东南风", b.now.windDir);
        assertEquals("2", b.now.windScale);
        assertEquals(68, b.now.humidity);
        assertEquals(at(10, 15), b.now.obsTime);
    }

    @Test
    public void hourlyAndDaily_parse() throws Exception {
        server.enqueue(json(200, HOURLY));
        server.enqueue(json(200, DAILY));
        QwHourlyResp h = api.hourly("101190104").blockingGet();
        QwDailyResp d = api.daily("101190104").blockingGet();
        assertEquals("/v7/weather/24h?location=101190104", server.takeRequest().getPath());
        assertEquals("/v7/weather/7d?location=101190104", server.takeRequest().getPath());

        WeatherBundle b = QWeatherMapper.toBundle(null, h, d, "101190104", "南京 江宁", 9L, ZONE);
        assertNull(b.now);
        assertEquals("101190104", b.locationId);
        assertEquals("南京 江宁", b.locationName);
        assertEquals(9L, b.fetchedAt);
        assertEquals(2, b.hourly.size());
        assertEquals(at(10, 16), b.hourly.get(0).time);
        assertEquals(0, b.hourly.get(0).pop);
        assertEquals(-1, b.hourly.get(1).pop);   // 空字符串 → 缺失
        assertEquals("3-4", b.hourly.get(1).windScale);
        assertEquals(0.3, b.hourly.get(1).precip, 1e-9);
        assertEquals(2, b.daily.size());
        assertEquals(at(10, 0), b.daily.get(0).date);
        assertEquals(26, b.daily.get(0).tempMax);
        assertEquals(18, b.daily.get(0).tempMin);
        assertEquals("多云", b.daily.get(0).textDay);
        assertEquals(72, b.daily.get(0).humidity);
        assertEquals(8.5, b.daily.get(1).precip, 1e-9);
        assertEquals("305", b.daily.get(1).iconDay);
    }

    @Test
    public void cityLookup_rangeCnNumber10_displayName() throws Exception {
        server.enqueue(json(200, CITY));
        List<CityOption> list = LocationResolver.search(api, "118.85,31.95", null).blockingGet();
        RecordedRequest req = server.takeRequest();
        assertEquals("/geo/v2/city/lookup?location=118.85%2C31.95&range=cn&number=10", req.getPath());
        assertEquals("test-key", req.getHeader("X-QW-Api-Key"));
        assertEquals(2, list.size());
        assertEquals("101190104", list.get(0).id);
        assertEquals("南京 江宁", list.get(0).displayName());
        assertEquals("南京", list.get(1).displayName());
    }

    @Test
    public void cityLookup_noResult_isEmptyList() {
        server.enqueue(json(200, "{\"code\":\"404\"}"));
        assertTrue(LocationResolver.search(api, "不存在", null).blockingGet().isEmpty());
        server.enqueue(json(400, "{\"error\":{\"status\":400,\"type\":"
                + "\"https://dev.qweather.com/docs/resource/error-code/#no-such-location\","
                + "\"title\":\"No Such Location\"}}"));
        assertTrue(LocationResolver.search(api, "不存在", null).blockingGet().isEmpty());
    }

    // —— 错误映射 ——

    private AppError errorOf(MockResponse response) {
        server.enqueue(response);
        try {
            QwNowResp r = api.now("101190104").blockingGet();
            QWeatherErrors.ensureOk(r.code);
            fail("expected error");
            return null;
        } catch (RuntimeException e) {
            return QWeatherErrors.toAppError(e);
        }
    }

    private static String problem(int status, String type, String title) {
        return "{\"error\":{\"status\":" + status + ",\"type\":\"https://dev.qweather.com/docs/resource/error-code/#"
                + type + "\",\"title\":\"" + title + "\",\"detail\":\"x\"}}";
    }

    @Test
    public void http401_unauthorized() {
        AppError e = errorOf(json(401, problem(401, "unauthorized", "Unauthorized")));
        assertEquals(AppError.Type.UNAUTHORIZED, e.type);
        assertEquals(401, e.httpCode);
        assertEquals(QWeatherErrors.MSG_UNAUTHORIZED, e.message);
    }

    @Test
    public void http403_byType() {
        assertEquals(QWeatherErrors.MSG_INVALID_HOST,
                errorOf(json(403, problem(403, "invalid-host", "Invalid Host"))).message);
        AppError credit = errorOf(json(403, problem(403, "no-credit", "No Credit")));
        assertEquals(QWeatherErrors.MSG_NO_CREDIT, credit.message);
        assertEquals(AppError.Type.NO_BALANCE, credit.type);
        assertEquals(QWeatherErrors.MSG_OVERDUE, errorOf(json(403, problem(403, "overdue", "Overdue"))).message);
        assertEquals(QWeatherErrors.MSG_SECURITY,
                errorOf(json(403, problem(403, "security-restriction", "Security Restriction"))).message);
        assertEquals(QWeatherErrors.MSG_DEPRECATED,
                errorOf(json(403, problem(403, "deprecated", "Deprecated"))).message);
        assertEquals(QWeatherErrors.MSG_FORBIDDEN, errorOf(json(403, "")).message);
    }

    @Test
    public void http429_and404_and500() {
        AppError tooMany = errorOf(json(429, problem(429, "too-many-requests", "Too Many Requests")));
        assertEquals(AppError.Type.RATE_LIMIT, tooMany.type);
        assertEquals(QWeatherErrors.MSG_TOO_MANY, tooMany.message);
        assertEquals(QWeatherErrors.MSG_MONTHLY_LIMIT,
                errorOf(json(429, problem(429, "over-monthly-limit", "Over Monthly Limit"))).message);
        assertEquals(QWeatherErrors.MSG_NOT_FOUND, errorOf(json(404, "")).message);
        assertEquals(QWeatherErrors.MSG_NO_SUCH_LOCATION,
                errorOf(json(400, problem(400, "no-such-location", "No Such Location"))).message);
        AppError server = errorOf(json(500, problem(500, "unknown-error", "Unknown Error")));
        assertEquals(AppError.Type.SERVER, server.type);
        assertEquals(QWeatherErrors.MSG_SERVER, server.message);
    }

    @Test
    public void legacyCodeInBody() {
        assertEquals(QWeatherErrors.MSG_UNAUTHORIZED, errorOf(json(200, "{\"code\":\"401\"}")).message);
        AppError e402 = errorOf(json(200, "{\"code\":\"402\"}"));
        assertEquals(AppError.Type.NO_BALANCE, e402.type);
        assertEquals(QWeatherErrors.MSG_NO_CREDIT, e402.message);
        assertEquals(QWeatherErrors.MSG_FORBIDDEN, errorOf(json(200, "{\"code\":\"403\"}")).message);
        assertEquals(QWeatherErrors.MSG_NO_SUCH_LOCATION, errorOf(json(200, "{\"code\":\"404\"}")).message);
        assertEquals(QWeatherErrors.MSG_NO_SUCH_LOCATION, errorOf(json(200, "{\"code\":\"204\"}")).message);
        assertEquals(QWeatherErrors.MSG_TOO_MANY, errorOf(json(200, "{\"code\":\"429\"}")).message);
        // HTTP 状态码 + 旧版 code
        assertEquals(QWeatherErrors.MSG_UNAUTHORIZED, errorOf(json(401, "{\"code\":\"401\"}")).message);
    }

    @Test
    public void networkFailure_usesOfflineMessage() throws Exception {
        server.shutdown();
        try {
            api.now("101190104").blockingGet();
            fail("expected error");
        } catch (RuntimeException e) {
            AppError err = QWeatherErrors.toAppError(e);
            assertEquals(AppError.Type.NETWORK, err.type);
            assertEquals("网络不太好，检查后再试一次", err.message);
        }
    }

    @Test
    public void wrappedExceptionsAreUserFacingForAppErrorFrom() {
        server.enqueue(json(401, problem(401, "unauthorized", "Unauthorized")));
        try {
            api.now("1").onErrorResumeNext(e -> io.reactivex.rxjava3.core.Single.error(QWeatherErrors.wrap(e)))
                    .blockingGet();
            fail("expected error");
        } catch (QWeatherException e) {
            assertEquals(QWeatherErrors.MSG_UNAUTHORIZED, AppError.from(e).message);
        }
    }

    @Test
    public void notConfigured_and_noLocation_areDetectable() {
        AppError nc = QWeatherErrors.notConfigured().toAppError();
        assertEquals("未配置天气服务，请在 local.properties 中填写和风天气的 API Host 和 Key", nc.message);
        assertTrue(QWeatherErrors.isNotConfigured(nc));
        AppError nl = QWeatherErrors.noLocation().toAppError();
        assertEquals("请选择地点", nl.message);
        assertTrue(QWeatherErrors.isNoLocation(nl));
        assertTrue(!QWeatherErrors.isNoLocation(nc));
    }
}
