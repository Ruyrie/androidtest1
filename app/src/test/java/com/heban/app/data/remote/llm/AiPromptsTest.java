package com.heban.app.data.remote.llm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.model.WeatherNow;
import com.heban.app.data.remote.llm.dto.ChatMessage;
import com.heban.app.data.remote.llm.dto.ChatRequest;

import org.junit.Test;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public class AiPromptsTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static AiPrompts.UserContext fullContext() {
        AiPrompts.UserContext c = new AiPrompts.UserContext();
        c.date = "2026年10月11日";
        c.solarTerm = "寒露后第 3 天";
        c.useRegionWeather = true;
        c.region = "南京市江宁区";
        c.weather = "今天多云转晴，18–26℃";
        c.useCrops = true;
        c.crops = "番茄（樱桃番茄，结果期第 68 天，1号大棚）";
        c.records = "10/08 番茄 施肥 硫酸钾 5kg";
        return c;
    }

    @Test
    public void system_containsRulesAndUserInfo() {
        String s = AiPrompts.system(AiPrompts.Style.DETAIL, fullContext());
        assertTrue(s.startsWith("你是「禾伴」App 里的农技助手，面向中国的种植户和家庭园艺用户。\n回答要求：\n"));
        assertTrue(s.contains("1. 使用简体中文。说明原因、步骤和注意事项，分点列出。\n"));
        assertTrue(s.contains("2. 涉及农药时，只写药剂通用名，并提醒「按产品标签用量使用、注意安全间隔期」，"));
        assertTrue(s.contains("4. 与种植无关的问题，简短回答后引导回种植话题。\n【用户信息】\n"));
        assertTrue(s.endsWith("【用户信息】\n地区：南京市江宁区；今天：2026年10月11日（寒露后第 3 天）\n"
                + "天气：今天多云转晴，18–26℃\n"
                + "在种作物：番茄（樱桃番茄，结果期第 68 天，1号大棚）\n"
                + "最近农事：10/08 番茄 施肥 硫酸钾 5kg"));
        assertFalse(s.contains("{"));
    }

    @Test
    public void styleRules() {
        assertTrue(AiPrompts.system(AiPrompts.Style.BRIEF, fullContext())
                .contains("1. 使用简体中文。先给结论，再列 3 条以内做法，200 字以内。\n"));
        assertTrue(AiPrompts.system(AiPrompts.Style.PLAIN, fullContext())
                .contains("1. 使用简体中文。少用术语，像农技员当面讲。\n"));
        assertEquals(AiPrompts.Style.BRIEF, AiPrompts.Style.of("BRIEF"));
        assertEquals(AiPrompts.Style.PLAIN, AiPrompts.Style.of("plain"));
        assertEquals(AiPrompts.Style.DETAIL, AiPrompts.Style.of(null));
        assertEquals(AiPrompts.Style.DETAIL, AiPrompts.Style.of("unknown"));
    }

    @Test
    public void regionWeatherOff_omitsRegionAndWeatherLine() {
        AiPrompts.UserContext c = fullContext();
        c.useRegionWeather = false;
        String s = AiPrompts.system(AiPrompts.Style.DETAIL, c);
        assertFalse(s.contains("地区："));
        assertFalse(s.contains("天气："));
        assertTrue(s.contains("\n今天：2026年10月11日（寒露后第 3 天）\n在种作物："));
    }

    @Test
    public void cropsOff_omitsCropAndRecordLines() {
        AiPrompts.UserContext c = fullContext();
        c.useCrops = false;
        String s = AiPrompts.system(AiPrompts.Style.DETAIL, c);
        assertFalse(s.contains("在种作物"));
        assertFalse(s.contains("最近农事"));
        assertTrue(s.endsWith("天气：今天多云转晴，18–26℃"));
    }

    @Test
    public void missingValues() {
        AiPrompts.UserContext c = fullContext();
        c.region = null;
        c.weather = null;
        c.solarTerm = null;
        c.crops = null;
        c.records = "";
        String s = AiPrompts.system(AiPrompts.Style.DETAIL, c);
        assertTrue(s.endsWith("【用户信息】\n今天：2026年10月11日\n在种作物：暂无\n最近农事：暂无"));
    }

    @Test
    public void dateOnly_forQuickAnswer() {
        String s = AiPrompts.system(AiPrompts.Style.BRIEF, AiPrompts.UserContext.dateOnly("2026年10月11日", "寒露"));
        assertTrue(s.endsWith("【用户信息】\n今天：2026年10月11日（寒露）"));
        ChatRequest req = AiPrompts.quickRequest("m", s, "番茄裂果怎么办");
        assertFalse(req.stream);
        assertEquals(2, req.messages.size());
        assertEquals(1024, (int) req.maxTokens);
    }

    private static ChatMessageEntity msg(long id, String role, String content) {
        ChatMessageEntity m = new ChatMessageEntity();
        m.id = id;
        m.role = role;
        m.content = content;
        m.createdAt = id;
        return m;
    }

    @Test
    public void history_keepsLastSixRounds() {
        List<ChatMessageEntity> list = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            list.add(msg(i * 2 - 1, ChatMessageEntity.ROLE_USER, "问" + i));
            list.add(msg(i * 2, ChatMessageEntity.ROLE_ASSISTANT, "答" + i));
        }
        List<ChatMessage> h = AiPrompts.history(list);
        assertEquals(12, h.size());
        assertEquals("问3", h.get(0).content);
        assertEquals("user", h.get(0).role);
        assertEquals("答3", h.get(1).content);
        assertEquals("assistant", h.get(1).role);
        assertEquals("答8", h.get(11).content);
    }

    @Test
    public void history_skipsUnansweredQuestions_andTruncates() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 2500; i++) longText.append('字');
        List<ChatMessageEntity> list = Arrays.asList(
                msg(1, ChatMessageEntity.ROLE_USER, "没回答的问题"),
                msg(2, ChatMessageEntity.ROLE_USER, longText.toString()),
                msg(3, ChatMessageEntity.ROLE_ASSISTANT, longText.toString()),
                msg(4, ChatMessageEntity.ROLE_USER, "失败的问题"),
                msg(5, ChatMessageEntity.ROLE_ASSISTANT, ""),
                msg(6, ChatMessageEntity.ROLE_USER, "看图"),
                msg(7, ChatMessageEntity.ROLE_ASSISTANT, "部分回答"));
        list.get(5).imagePath = "file:///data/x.jpg";
        List<ChatMessage> h = AiPrompts.history(list);
        assertEquals(4, h.size());
        assertEquals(2000, ((String) h.get(0).content).length());
        assertEquals(2000, ((String) h.get(1).content).length());
        assertEquals("[图片] 看图", h.get(2).content);
        assertEquals("部分回答", h.get(3).content);
    }

    @Test
    public void chatRequest_truncatesQuestion_andOrdersMessages() {
        StringBuilder q = new StringBuilder();
        for (int i = 0; i < 2100; i++) q.append('问');
        List<ChatMessage> history = Arrays.asList(ChatMessage.user("前一问"), ChatMessage.assistant("前一答"));
        ChatRequest req = AiPrompts.chatRequest("deepseek-v4-flash", "SYS", history, q.toString());
        assertTrue(req.stream);
        assertEquals(1024, (int) req.maxTokens);
        assertEquals(0.7, req.temperature, 1e-9);
        assertEquals(4, req.messages.size());
        assertEquals("system", req.messages.get(0).role);
        assertEquals("SYS", req.messages.get(0).content);
        assertEquals("前一问", req.messages.get(1).content);
        assertEquals(2000, ((String) req.messages.get(3).content).length());
    }

    @Test
    public void imageChatRequest_imageOnlyUsesDefaultQuestion() {
        ChatRequest req = AiPrompts.imageChatRequest("vl", "SYS", Collections.emptyList(), "  ", "data:x");
        assertFalse(req.stream);
        assertEquals(AiPrompts.IMAGE_ONLY_QUESTION, req.messages.get(1).textContent());
    }

    @Test
    public void truncate_and_title() {
        assertEquals("abc", AiPrompts.truncate("abcdef", 3));
        assertEquals("ab", AiPrompts.truncate("ab", 3));
        assertEquals("", AiPrompts.truncate(null, 3));
        assertEquals("😀😀", AiPrompts.truncate("😀😀😀", 2));
        assertEquals("番茄叶片发黄怎么办？", AiPrompts.sessionTitle("  番茄叶片发黄怎么办？ "));
        assertEquals("一二三四五六七八九十一二三四五六七八九十",
                AiPrompts.sessionTitle("一二三四五六七八九十一二三四五六七八九十多出来的字"));
        assertEquals("第一行 第二行", AiPrompts.sessionTitle("第一行\n第二行"));
        assertEquals("图片提问", AiPrompts.sessionTitle(""));
    }

    @Test
    public void formatRegion_districtLevel() {
        assertEquals("南京市江宁区", AiPrompts.formatRegion("江苏省 南京市 江宁区"));
        assertEquals("北京市朝阳区", AiPrompts.formatRegion("北京市 朝阳区"));
        assertEquals("北京市朝阳区", AiPrompts.formatRegion("北京市 北京市 朝阳区"));
        assertEquals("南京市江宁区", AiPrompts.formatRegion("江苏省南京市江宁区"));
        assertEquals("南宁市西乡塘区", AiPrompts.formatRegion("广西壮族自治区南宁市西乡塘区"));
        assertEquals("寿光市", AiPrompts.formatRegion("寿光市"));
        assertNull(AiPrompts.formatRegion("  "));
        assertNull(AiPrompts.formatRegion(null));
    }

    @Test
    public void solarTerms_parseAndLabel() {
        NavigableMap<LocalDate, String> terms = AiPrompts.parseSolarTerms(new StringReader(
                "{\"tips\":{\"寒露\":\"x\"},\"terms\":[{\"date\":\"2026-10-08\",\"name\":\"寒露\"},"
                        + "{\"date\":\"2026-10-23\",\"name\":\"霜降\"},{\"date\":\"bad\",\"name\":\"x\"},{\"name\":\"y\"}]}"));
        assertEquals(2, terms.size());
        assertEquals("寒露", AiPrompts.solarTermLabel(terms, LocalDate.of(2026, 10, 8)));
        assertEquals("寒露后第 3 天", AiPrompts.solarTermLabel(terms, LocalDate.of(2026, 10, 11)));
        assertEquals("霜降", AiPrompts.solarTermLabel(terms, LocalDate.of(2026, 10, 23)));
        assertNull(AiPrompts.solarTermLabel(terms, LocalDate.of(2026, 10, 1)));
        assertNull(AiPrompts.solarTermLabel(terms, LocalDate.of(2027, 3, 1)));
        assertTrue(AiPrompts.parseSolarTerms(new StringReader("not json")).isEmpty());
        assertEquals("2026年10月11日", AiPrompts.formatDate(LocalDate.of(2026, 10, 11)));
    }

    private static long dayMillis(int month, int day) {
        return LocalDateTime.of(2026, month, day, 0, 0).atZone(ZONE).toInstant().toEpochMilli();
    }

    private static DailyWeather daily(int day, String textDay, String textNight, int min, int max) {
        DailyWeather d = new DailyWeather();
        d.date = dayMillis(10, day);
        d.textDay = textDay;
        d.textNight = textNight;
        d.tempMin = min;
        d.tempMax = max;
        d.windScaleDay = "1-2";
        return d;
    }

    @Test
    public void weatherSummary_todayAndNextThreeDays() {
        WeatherBundle b = new WeatherBundle();
        b.daily.add(daily(10, "晴", "晴", 17, 25));
        b.daily.add(daily(11, "多云", "晴", 18, 26));
        b.daily.add(daily(12, "小雨", "小雨", 8, 15));
        b.daily.add(daily(13, "阴", "阴", 10, 16));
        b.daily.add(daily(14, "晴", "晴", 9, 18));
        b.daily.add(daily(15, "晴", "晴", 9, 19));
        assertEquals("今天多云转晴，18–26℃，风力 1-2 级；未来 3 天：10/12 小雨 8–15℃，10/13 阴 10–16℃，10/14 晴 9–18℃",
                AiPrompts.weatherSummary(b, LocalDate.of(2026, 10, 11), ZONE));
    }

    @Test
    public void weatherSummary_nowOnly_or_none() {
        WeatherBundle b = new WeatherBundle();
        b.now = new WeatherNow();
        b.now.text = "多云";
        b.now.temp = 24;
        b.now.windScale = "2";
        assertEquals("现在多云，24℃，风力 2 级", AiPrompts.weatherSummary(b, LocalDate.of(2026, 10, 11), ZONE));
        assertNull(AiPrompts.weatherSummary(new WeatherBundle(), LocalDate.of(2026, 10, 11), ZONE));
        assertNull(AiPrompts.weatherSummary(null, LocalDate.of(2026, 10, 11), ZONE));
    }

    @Test
    public void cropLines() {
        CropCard c = new CropCard();
        c.name = "番茄";
        c.variety = "樱桃番茄";
        c.stage = "结果期";
        c.daysPlanted = 68;
        c.plotName = "1号大棚";
        assertEquals("番茄（樱桃番茄，结果期第 68 天，1号大棚）", AiPrompts.cropLine(c));
        CropCard d = new CropCard();
        d.name = "黄瓜";
        assertEquals("黄瓜", AiPrompts.cropLine(d));
        assertEquals("番茄（樱桃番茄，结果期第 68 天，1号大棚）；黄瓜", AiPrompts.cropsText(Arrays.asList(c, d)));
        assertNull(AiPrompts.cropsText(Collections.emptyList()));
    }

    private static FarmRecordEntity record(long id, long cropId, String type, int day, String material, double amount,
                                           String unit) {
        FarmRecordEntity r = new FarmRecordEntity();
        r.id = id;
        r.cropId = cropId;
        r.type = type;
        r.date = LocalDateTime.of(2026, 10, day, 9, 0).atZone(ZONE).toInstant().toEpochMilli();
        r.material = material;
        r.amount = amount;
        r.unit = unit;
        return r;
    }

    @Test
    public void recentRecords_latestThreeFormatted() {
        List<FarmRecordEntity> all = Arrays.asList(
                record(1, 1, "FERTILIZE", 8, "硫酸钾", 5, "kg"),
                record(2, 2, "SPRAY", 9, "烯酰吗啉", 0, null),
                record(3, 1, "IRRIGATE", 2, "滴灌", 0, null),
                record(4, 2, "OTHER", 10, "拍照识别：黄瓜霜霉病（中度）", 0, null),
                record(5, 1, "HARVEST", 5, null, 12.5, "kg"));
        List<FarmRecordEntity> latest = AiPrompts.latestRecords(all, 3);
        assertEquals(Arrays.asList(4L, 2L, 1L), Arrays.asList(latest.get(0).id, latest.get(1).id, latest.get(2).id));
        Map<Long, String> names = new HashMap<>();
        names.put(1L, "番茄");
        names.put(2L, "黄瓜");
        assertEquals("10/10 黄瓜 拍照识别：黄瓜霜霉病（中度）；10/09 黄瓜 用药 烯酰吗啉；10/08 番茄 施肥 硫酸钾 5kg",
                AiPrompts.recordsText(latest, names, ZONE));
        assertEquals("10/05 番茄 采收 12.5kg", AiPrompts.recordLine(all.get(4), "番茄", ZONE));
        assertNull(AiPrompts.recordsText(Collections.emptyList(), names, ZONE));
    }
}
