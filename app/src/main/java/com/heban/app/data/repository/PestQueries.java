package com.heban.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.PestEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 病虫害百科的查询规则（纯 Java，LocalPestRepository 使用，可直接做单元测试）。
 * 百科只有几十条，全部在内存中筛选。
 */
public final class PestQueries {
    public static final String LEVEL_HIGH = "高发";
    public static final String LEVEL_ELEVATED = "较高";
    /** 名称包含匹配的最短关键字长度 */
    public static final int MIN_MATCH_LENGTH = 2;
    /** crop_tags 中表示一类作物的词，不作为 D04「作物」筛选选项（按 crops 文字仍能筛到） */
    static final Set<String> GROUP_TAGS = new HashSet<>(Arrays.asList("多种蔬菜", "瓜类", "茄果"));

    private PestQueries() {
    }

    // —— 文本工具 ——

    /** 按顿号、逗号拆分（别名、作物标签），去掉空白和空项 */
    public static List<String> splitList(@Nullable String text) {
        if (text == null || text.trim().isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String s : text.split("[、,，;；]")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** months 字段（如「3,4,5,9,10,11」）→ 月份集合；非法项忽略 */
    public static Set<Integer> months(@Nullable String months) {
        Set<Integer> out = new TreeSet<>();
        for (String s : splitList(months)) {
            try {
                int m = Integer.parseInt(s);
                if (m >= 1 && m <= 12) out.add(m);
            } catch (NumberFormatException ignored) {
                // 忽略非法月份
            }
        }
        return out;
    }

    public static boolean inSeason(@NonNull PestEntity p, int month) {
        return months(p.months).contains(month);
    }

    private static boolean containsIgnoreCase(@Nullable String text, @NonNull String lowerNeedle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }

    @Nullable
    private static String normalize(@Nullable String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // —— D04 搜索与筛选 ——

    /** keyword：名称 / 别名 / 作物标签 / 危害作物 / 英文名，忽略大小写的包含匹配 */
    public static boolean matchesKeyword(@NonNull PestEntity p, @Nullable String keyword) {
        String kw = normalize(keyword);
        if (kw == null) return true;
        String k = kw.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(p.name, k) || containsIgnoreCase(p.aliases, k)
                || containsIgnoreCase(p.cropTags, k) || containsIgnoreCase(p.crops, k)
                || containsIgnoreCase(p.enName, k);
    }

    /** kind：病害 / 虫害 / 生理性；null、空或「全部」不过滤 */
    public static boolean matchesKind(@NonNull PestEntity p, @Nullable String kind) {
        String k = normalize(kind);
        return k == null || "全部".equals(k) || k.equals(p.kind);
    }

    /** crop：作物标签包含，或危害作物文字包含（如「黄瓜」也能筛到「瓜类」白粉病、「多种蔬菜」蚜虫） */
    public static boolean matchesCrop(@NonNull PestEntity p, @Nullable String crop) {
        String c = normalize(crop);
        if (c == null || "全部".equals(c)) return true;
        String k = c.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(p.cropTags, k) || containsIgnoreCase(p.crops, k);
    }

    /** D04 列表：保持原顺序（id） */
    public static List<PestEntity> filter(@NonNull List<PestEntity> all, @Nullable String keyword,
                                          @Nullable String kind, @Nullable String crop) {
        List<PestEntity> out = new ArrayList<>();
        for (PestEntity p : all) {
            if (matchesKind(p, kind) && matchesCrop(p, crop) && matchesKeyword(p, keyword)) out.add(p);
        }
        return out;
    }

    // —— D01 / D04 本季高发 ——

    /** 高发 0、较高 1、其他 2 */
    public static int levelRank(@Nullable String level) {
        if (LEVEL_HIGH.equals(level)) return 0;
        if (LEVEL_ELEVATED.equals(level)) return 1;
        return 2;
    }

    public static final Comparator<PestEntity> SEASON_ORDER = (a, b) -> {
        int c = Integer.compare(levelRank(a.seasonLevel), levelRank(b.seasonLevel));
        return c != 0 ? c : Long.compare(a.id, b.id);
    };

    /** months 包含 month 的条目，高发在前，同级按 id */
    public static List<PestEntity> seasonal(@NonNull List<PestEntity> all, int month) {
        List<PestEntity> out = new ArrayList<>();
        for (PestEntity p : all) {
            if (inSeason(p, month)) out.add(p);
        }
        Collections.sort(out, SEASON_ORDER);
        return out;
    }

    // —— 识别结果跳百科 ——

    /** 去掉末尾的括号说明，如「缺镁症（生理性）」→「缺镁症」 */
    static String stripNote(String s) {
        String t = s.replaceAll("[（(][^（()）]*[）)]\\s*$", "").trim();
        return t.isEmpty() ? s : t;
    }

    /**
     * 名称匹配，依次：名称完全相同 → 别名或英文名相同 → 名称 / 别名出现在查询中（取最长的，至少 2 个字）
     * → 名称 / 别名包含查询（查询至少 2 个字，取名称最短的）。找不到返回 null。
     */
    @Nullable
    public static PestEntity match(@NonNull List<PestEntity> all, @Nullable String query) {
        String q0 = normalize(query);
        if (q0 == null) return null;
        String[] queries = {q0, stripNote(q0)};
        for (String q : queries) {
            for (PestEntity p : all) {
                if (q.equals(p.name)) return p;
            }
        }
        for (String q : queries) {
            for (PestEntity p : all) {
                if (p.enName != null && q.equalsIgnoreCase(p.enName.trim())) return p;
                if (splitList(p.aliases).contains(q)) return p;
            }
        }
        // 名称 / 别名出现在查询中，如「黄瓜霜霉病（中度）」「疑似番茄灰霉病菌」
        PestEntity best = null;
        int bestLen = 0;
        for (PestEntity p : all) {
            for (String term : terms(p)) {
                if (term.length() >= MIN_MATCH_LENGTH && term.length() > bestLen && q0.contains(term)) {
                    best = p;
                    bestLen = term.length();
                }
            }
        }
        if (best != null) return best;
        // 名称 / 别名包含查询，如「灰霉」→ 番茄灰霉病
        String q = stripNote(q0);
        if (q.length() < MIN_MATCH_LENGTH) return null;
        best = null;
        for (PestEntity p : all) {
            boolean hit = false;
            for (String term : terms(p)) {
                if (term.contains(q)) {
                    hit = true;
                    break;
                }
            }
            if (hit && (best == null || p.name.length() < best.name.length())) best = p;
        }
        return best;
    }

    private static List<String> terms(PestEntity p) {
        List<String> out = new ArrayList<>();
        out.add(p.name);
        out.addAll(splitList(p.aliases));
        return out;
    }

    // —— D04 作物筛选选项 ——

    /** crop_tags 中出现过的作物（去重，不含「多种蔬菜」等类别词），按出现次数从多到少，同次数按首次出现 */
    public static List<String> cropOptions(@NonNull List<PestEntity> all) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PestEntity p : all) {
            for (String tag : splitList(p.cropTags)) {
                if (GROUP_TAGS.contains(tag)) continue;
                Integer n = counts.get(tag);
                counts.put(tag, n == null ? 1 : n + 1);
            }
        }
        List<String> out = new ArrayList<>(counts.keySet());
        List<String> firstSeen = new ArrayList<>(out);
        Collections.sort(out, (a, b) -> {
            int c = Integer.compare(counts.get(b), counts.get(a));
            return c != 0 ? c : Integer.compare(firstSeen.indexOf(a), firstSeen.indexOf(b));
        });
        return out;
    }
}
