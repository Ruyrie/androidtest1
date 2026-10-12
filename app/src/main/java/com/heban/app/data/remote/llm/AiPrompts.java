package com.heban.app.data.remote.llm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.heban.app.data.local.entity.ChatMessageEntity;
import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.model.CropCard;
import com.heban.app.data.model.DailyWeather;
import com.heban.app.data.model.FarmType;
import com.heban.app.data.model.WeatherBundle;
import com.heban.app.data.remote.llm.dto.ChatMessage;
import com.heban.app.data.remote.llm.dto.ChatRequest;

import java.io.Reader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * AI 助手的提示词与上下文（技术准备文档 9.4）：固定的角色和安全要求 + 按 M11 设置附带的用户信息；
 * 对话历史只带最近 6 轮（12 条消息），超长内容截断到 2000 字；问答 max_tokens 1024。
 */
public final class AiPrompts {
    public static final int MAX_TOKENS = 1024;
    public static final double TEMPERATURE = 0.7;
    public static final int HISTORY_ROUNDS = 6;
    public static final int MAX_CHARS = 2000;
    public static final int TITLE_CHARS = 20;
    public static final int RECENT_RECORDS = 3;
    static final int MAX_CROPS = 10;

    /** 回答风格（SettingsStore.AI_STYLE），默认详细 */
    public enum Style {
        BRIEF("先给结论，再列 3 条以内做法，200 字以内。"),
        DETAIL("说明原因、步骤和注意事项，分点列出。"),
        PLAIN("少用术语，像农技员当面讲。");

        /** 表 9-2 中的 {style_rule} */
        public final String rule;

        Style(String rule) {
            this.rule = rule;
        }

        @NonNull
        public static Style of(@Nullable String name) {
            if (name != null) {
                for (Style s : values()) {
                    if (s.name().equalsIgnoreCase(name.trim())) return s;
                }
            }
            return DETAIL;
        }
    }

    /** 系统提示词模板中【用户信息】之前的固定部分（AiPrompts.SYSTEM） */
    public static final String SYSTEM = "你是「禾伴」App 里的农技助手，面向中国的种植户和家庭园艺用户。\n"
            + "回答要求：\n"
            + "1. 使用简体中文。{style_rule}\n"
            + "2. 涉及农药时，只写药剂通用名，并提醒「按产品标签用量使用、注意安全间隔期」，\n"
            + "   不要编造具体用量；拿不准时建议咨询当地农技站。\n"
            + "3. 结合下面的用户信息回答；信息不够时先给出最可能的判断，再说明还需要补充什么\n"
            + "   （例如照片、发病部位、最近施肥用药情况）。\n"
            + "4. 与种植无关的问题，简短回答后引导回种植话题。\n"
            + "【用户信息】";

    /** 默认的图片提问文字（用户只发了图片时） */
    public static final String IMAGE_ONLY_QUESTION = "请看看这张照片里的作物有什么问题，该怎么处理？";

    /**
     * 【用户信息】中的变量。useRegionWeather 对应 M11「所在地区与天气」（关闭时省略地区与天气整行），
     * useCrops 对应「我的作物与农事记录」（关闭时省略作物与农事两行）。
     */
    public static final class UserContext {
        /** 「2026年10月11日」 */
        @NonNull public String date = "";
        /** 「寒露」或「寒露后第 3 天」，可为空 */
        @Nullable public String solarTerm;
        public boolean useRegionWeather;
        /** 只到区县，如「南京市江宁区」 */
        @Nullable public String region;
        @Nullable public String weather;
        public boolean useCrops;
        @Nullable public String crops;
        @Nullable public String records;

        /** 只有日期与节气（一次性回答使用，不附带用户的作物与天气信息） */
        public static UserContext dateOnly(@NonNull String date, @Nullable String solarTerm) {
            UserContext c = new UserContext();
            c.date = date;
            c.solarTerm = solarTerm;
            return c;
        }
    }

    private AiPrompts() {
    }

    // ———————————————————————— 系统提示词 ————————————————————————

    @NonNull
    public static String system(@NonNull Style style, @NonNull UserContext ctx) {
        StringBuilder sb = new StringBuilder(SYSTEM.replace("{style_rule}", style.rule));
        StringBuilder line = new StringBuilder();
        if (ctx.useRegionWeather && notBlank(ctx.region)) line.append("地区：").append(ctx.region.trim()).append("；");
        line.append("今天：").append(ctx.date);
        if (notBlank(ctx.solarTerm)) line.append("（").append(ctx.solarTerm.trim()).append("）");
        sb.append('\n').append(line);
        if (ctx.useRegionWeather && notBlank(ctx.weather)) sb.append("\n天气：").append(ctx.weather.trim());
        if (ctx.useCrops) {
            sb.append("\n在种作物：").append(notBlank(ctx.crops) ? ctx.crops.trim() : "暂无");
            sb.append("\n最近农事：").append(notBlank(ctx.records) ? ctx.records.trim() : "暂无");
        }
        return sb.toString();
    }

    // ———————————————————————— 请求 ————————————————————————

    /** 流式问答请求：系统提示词 + 最近 6 轮历史 + 本次问题 */
    @NonNull
    public static ChatRequest chatRequest(@NonNull String model, @NonNull String system,
                                          @NonNull List<ChatMessage> history, @NonNull String question) {
        ChatRequest req = base(model, system, history);
        req.stream = true;
        req.messages.add(ChatMessage.user(truncate(question, MAX_CHARS)));
        return req;
    }

    /** 带图片的提问：用识图模型非流式回答，提示词同问答（不要求 JSON） */
    @NonNull
    public static ChatRequest imageChatRequest(@NonNull String model, @NonNull String system,
                                               @NonNull List<ChatMessage> history, @Nullable String question,
                                               @NonNull String dataUrl) {
        ChatRequest req = base(model, system, history);
        req.stream = false;
        String q = notBlank(question) ? truncate(question.trim(), MAX_CHARS) : IMAGE_ONLY_QUESTION;
        req.messages.add(ChatMessage.userWithImage(dataUrl, q));
        return req;
    }

    /** 一次性回答（不带历史） */
    @NonNull
    public static ChatRequest quickRequest(@NonNull String model, @NonNull String system, @NonNull String question) {
        ChatRequest req = chatRequest(model, system, Collections.emptyList(), question);
        req.stream = false;
        return req;
    }

    private static ChatRequest base(String model, String system, List<ChatMessage> history) {
        ChatRequest req = new ChatRequest();
        req.model = model;
        req.maxTokens = MAX_TOKENS;
        req.temperature = TEMPERATURE;
        req.messages.add(ChatMessage.system(system));
        req.messages.addAll(history);
        return req;
    }

    /**
     * 对话历史：只取「用户提问 + 有内容的回答」成对的轮次（未得到回答的提问跳过，保证角色交替），
     * 最多最近 6 轮，每条截断到 2000 字。以前附带的图片不再发送，只在文字前标注「[图片]」。
     *
     * @param previous 本次问题之前的消息，按时间顺序
     */
    @NonNull
    public static List<ChatMessage> history(@NonNull List<ChatMessageEntity> previous) {
        List<ChatMessage[]> rounds = new ArrayList<>();
        for (int i = 0; i + 1 < previous.size(); i++) {
            ChatMessageEntity q = previous.get(i);
            ChatMessageEntity a = previous.get(i + 1);
            if (!ChatMessageEntity.ROLE_USER.equals(q.role) || !ChatMessageEntity.ROLE_ASSISTANT.equals(a.role)) {
                continue;
            }
            if (a.content == null || a.content.trim().isEmpty()) continue;
            String qText = q.content == null ? "" : q.content.trim();
            if (q.imagePath != null && !q.imagePath.isEmpty()) {
                qText = "[图片] " + (qText.isEmpty() ? IMAGE_ONLY_QUESTION : qText);
            }
            if (qText.isEmpty()) continue;
            rounds.add(new ChatMessage[]{
                    ChatMessage.user(truncate(qText, MAX_CHARS)),
                    ChatMessage.assistant(truncate(a.content.trim(), MAX_CHARS))});
            i++;
        }
        int from = Math.max(0, rounds.size() - HISTORY_ROUNDS);
        List<ChatMessage> out = new ArrayList<>();
        for (ChatMessage[] r : rounds.subList(from, rounds.size())) {
            out.add(r[0]);
            out.add(r[1]);
        }
        return out;
    }

    /** 超过 max 个字符时截断（按 Unicode 码点，不拆开代理对） */
    @NonNull
    public static String truncate(@Nullable String s, int max) {
        if (s == null) return "";
        if (s.codePointCount(0, s.length()) <= max) return s;
        return s.substring(0, s.offsetByCodePoints(0, max));
    }

    /** 会话标题：第一个问题的前 20 个字（空白合并为一个空格）；问题为空时为「图片提问」 */
    @NonNull
    public static String sessionTitle(@Nullable String question) {
        String q = question == null ? "" : question.trim().replaceAll("\\s+", " ");
        if (q.isEmpty()) return "图片提问";
        return truncate(q, TITLE_CHARS);
    }

    // ———————————————————————— 用户信息格式化 ————————————————————————

    /** 「2026年10月11日」 */
    @NonNull
    public static String formatDate(@NonNull LocalDate d) {
        return d.getYear() + "年" + d.getMonthValue() + "月" + d.getDayOfMonth() + "日";
    }

    /**
     * 地区只到区县：「江苏省 南京市 江宁区」→「南京市江宁区」；「北京市 朝阳区」→「北京市朝阳区」。
     * 空值返回 null。
     */
    @Nullable
    public static String formatRegion(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String[] parts = raw.trim().split("[\\s/·,，]+");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).equals(p)) continue;
            tokens.add(p);
        }
        if (tokens.isEmpty()) return null;
        if (tokens.size() >= 3) return tokens.get(tokens.size() - 2) + tokens.get(tokens.size() - 1);
        if (tokens.size() == 1) {
            String t = tokens.get(0);
            int p = t.indexOf("自治区");
            int len = 3;
            if (p < 0) {
                p = t.indexOf('省');
                len = 1;
            }
            if (p > 0 && p + len < t.length()) return t.substring(p + len);
            return t;
        }
        return tokens.get(0) + tokens.get(1);
    }

    /** 解析 assets/mock/solar_terms.json 中的 terms（日期 → 节气名）；文件损坏时返回空表 */
    @NonNull
    public static NavigableMap<LocalDate, String> parseSolarTerms(@NonNull Reader reader) {
        NavigableMap<LocalDate, String> map = new TreeMap<>();
        TermsJson json;
        try {
            json = new Gson().fromJson(reader, TermsJson.class);
        } catch (JsonParseException e) {
            return map;
        }
        if (json == null || json.terms == null) return map;
        for (TermEntry e : json.terms) {
            if (e == null || e.date == null || e.name == null || e.name.isEmpty()) continue;
            try {
                map.put(LocalDate.parse(e.date), e.name);
            } catch (DateTimeParseException ignored) {
                // 跳过格式错误的日期
            }
        }
        return map;
    }

    static final class TermsJson {
        List<TermEntry> terms;
    }

    static final class TermEntry {
        String date;
        String name;
    }

    /** 节气说明：当天为节气时「寒露」，否则「寒露后第 3 天」；超出数据范围时为 null */
    @Nullable
    public static String solarTermLabel(@NonNull NavigableMap<LocalDate, String> terms, @NonNull LocalDate day) {
        Map.Entry<LocalDate, String> e = terms.floorEntry(day);
        if (e == null) return null;
        long days = ChronoUnit.DAYS.between(e.getKey(), day);
        if (days == 0) return e.getValue();
        if (days > 16) return null;   // 节气间隔约 15 天，超过说明数据不全
        return e.getValue() + "后第 " + days + " 天";
    }

    /**
     * 天气摘要：今天天气、温度区间、风力，以及未来 3 天要点，如
     * 「今天多云转晴，18–26℃，风力 1-2 级；未来 3 天：10/12 小雨 8–15℃，10/13 阴 10–16℃，10/14 晴 9–18℃」。
     * 没有数据时为 null。
     */
    @Nullable
    public static String weatherSummary(@Nullable WeatherBundle b, @NonNull LocalDate today, @NonNull ZoneId zone) {
        if (b == null) return null;
        DailyWeather todayW = null;
        List<DailyWeather> next = new ArrayList<>();
        if (b.daily != null) {
            for (DailyWeather d : b.daily) {
                if (d == null) continue;
                LocalDate day = Instant.ofEpochMilli(d.date).atZone(zone).toLocalDate();
                if (day.equals(today)) {
                    todayW = d;
                } else if (day.isAfter(today) && next.size() < 3) {
                    next.add(d);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        if (todayW != null) {
            sb.append("今天").append(nz(todayW.summaryText())).append("，")
                    .append(todayW.tempMin).append("–").append(todayW.tempMax).append("℃");
            if (notBlank(todayW.windScaleDay)) sb.append("，风力 ").append(todayW.windScaleDay.trim()).append(" 级");
        } else if (b.now != null) {
            sb.append("现在").append(nz(b.now.text)).append("，").append(b.now.temp).append("℃");
            if (notBlank(b.now.windScale)) sb.append("，风力 ").append(b.now.windScale.trim()).append(" 级");
        }
        if (!next.isEmpty()) {
            if (sb.length() > 0) sb.append("；");
            sb.append("未来 ").append(next.size()).append(" 天：");
            for (int i = 0; i < next.size(); i++) {
                DailyWeather d = next.get(i);
                LocalDate day = Instant.ofEpochMilli(d.date).atZone(zone).toLocalDate();
                if (i > 0) sb.append("，");
                sb.append(mmdd(day)).append(' ').append(nz(d.summaryText())).append(' ')
                        .append(d.tempMin).append("–").append(d.tempMax).append("℃");
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** 「番茄（樱桃番茄，结果期第 68 天，1号大棚）」 */
    @NonNull
    public static String cropLine(@NonNull CropCard c) {
        List<String> parts = new ArrayList<>();
        if (notBlank(c.variety)) parts.add(c.variety.trim());
        String days = c.daysPlanted > 0 ? "第 " + c.daysPlanted + " 天" : "";
        if (notBlank(c.stage)) parts.add(c.stage.trim() + days);
        else if (!days.isEmpty()) parts.add("种植" + days);
        if (notBlank(c.plotName)) parts.add(c.plotName.trim());
        String name = nz(c.name).trim();
        return parts.isEmpty() ? name : name + "（" + join("，", parts) + "）";
    }

    /** 在种作物（最多 10 种），没有时为 null */
    @Nullable
    public static String cropsText(@Nullable List<CropCard> cards) {
        if (cards == null || cards.isEmpty()) return null;
        List<String> lines = new ArrayList<>();
        for (CropCard c : cards) {
            if (c == null) continue;
            lines.add(cropLine(c));
            if (lines.size() >= MAX_CROPS) break;
        }
        return lines.isEmpty() ? null : join("；", lines);
    }

    /** 按作业日期取最近 n 条农事记录 */
    @NonNull
    public static List<FarmRecordEntity> latestRecords(@NonNull List<FarmRecordEntity> all, int n) {
        List<FarmRecordEntity> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparingLong((FarmRecordEntity r) -> r.date)
                .thenComparingLong(r -> r.id).reversed());
        return sorted.size() > n ? new ArrayList<>(sorted.subList(0, n)) : sorted;
    }

    /** 「10/08 番茄 施肥 硫酸钾 5kg」（类型为其他时不写类型） */
    @NonNull
    public static String recordLine(@NonNull FarmRecordEntity r, @Nullable String cropName, @NonNull ZoneId zone) {
        StringBuilder sb = new StringBuilder(mmdd(Instant.ofEpochMilli(r.date).atZone(zone).toLocalDate()));
        if (notBlank(cropName)) sb.append(' ').append(cropName.trim());
        FarmType type = FarmType.of(r.type);
        if (type != FarmType.OTHER || !notBlank(r.material)) sb.append(' ').append(type.label);
        if (notBlank(r.material)) sb.append(' ').append(r.material.trim());
        if (r.amount > 0) {
            sb.append(' ').append(formatAmount(r.amount));
            if (notBlank(r.unit)) sb.append(r.unit.trim());
        }
        return sb.toString();
    }

    /** 最近农事（已按时间倒序），没有时为 null */
    @Nullable
    public static String recordsText(@Nullable List<FarmRecordEntity> records, @NonNull Map<Long, String> cropNames,
                                     @NonNull ZoneId zone) {
        if (records == null || records.isEmpty()) return null;
        List<String> lines = new ArrayList<>();
        for (FarmRecordEntity r : records) {
            if (r != null) lines.add(recordLine(r, cropNames.get(r.cropId), zone));
        }
        return lines.isEmpty() ? null : join("；", lines);
    }

    static String formatAmount(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    private static String mmdd(LocalDate d) {
        return String.format(Locale.ROOT, "%02d/%02d", d.getMonthValue(), d.getDayOfMonth());
    }

    private static String join(String sep, List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    private static boolean notBlank(@Nullable String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String nz(@Nullable String s) {
        return s == null ? "" : s;
    }
}
