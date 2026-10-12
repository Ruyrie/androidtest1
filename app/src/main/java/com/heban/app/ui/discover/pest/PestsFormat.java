package com.heban.app.ui.discover.pest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.heban.app.data.local.entity.PestEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 病虫害百科（D01 / D04 / D05）的文字处理：JSON 数组解析、卡片副标题、横幅说明、化学防治提醒。 */
public final class PestsFormat {
    /** 本季高发程度：高发（Error 配色），其余（较高）用 Warning 配色 */
    public static final String LEVEL_HIGH = "高发";

    private PestsFormat() {
    }

    /** PestEntity 中 images / image_captions / prevention_* 的 JSON 字符串数组；空值或格式错误返回空列表 */
    @NonNull
    public static List<String> jsonList(@NonNull Gson gson, @Nullable String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            JsonElement root = gson.fromJson(json, JsonElement.class);
            if (root == null || !root.isJsonArray()) return Collections.emptyList();
            JsonArray arr = root.getAsJsonArray();
            List<String> out = new ArrayList<>(arr.size());
            for (JsonElement e : arr) {
                if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) continue;
                String s = e.getAsString().trim();
                if (!s.isEmpty()) out.add(s);
            }
            return out;
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            return Collections.emptyList();
        }
    }

    /** 把顿号 / 逗号分隔的标签拆开 */
    @NonNull
    public static List<String> tags(@Nullable String tags) {
        if (tags == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String t : tags.split("[、,，]")) {
            String s = t.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    /** D04 卡片副标题：「黄瓜 · 卵菌病害」「瓜类、草莓 · 真菌病害」 */
    @NonNull
    public static String cardSubtitle(@NonNull PestEntity p) {
        return join(" · ", p.cropTags, p.category);
    }

    /**
     * D04「本季高发」横幅说明：「黄瓜、白菜等 · 秋季低温高湿易发」。
     * 有多种作物时加「等」；只有一种（如「多种蔬菜」）时不加。
     */
    @NonNull
    public static String bannerDesc(@NonNull PestEntity p) {
        List<String> t = tags(p.cropTags);
        // 数据中的提示已带作物（如「黄瓜、白菜等 · 秋季低温高湿易发」）时直接使用，避免重复
        if (p.tip != null && p.tip.contains(" · ")) return p.tip;
        String crops = t.isEmpty() ? null : String.join("、", t) + (t.size() > 1 ? "等" : "");
        return join(" · ", crops, p.tip);
    }

    /** 本季高发程度是否为「高发」 */
    public static boolean isHigh(@Nullable String level) {
        return level != null && LEVEL_HIGH.equals(level.trim());
    }

    /**
     * 化学防治列表：数据里没有提到「标签」用量时，末尾补一条「按产品标签用量使用、注意安全间隔期」；
     * 没有任何化学防治措施时保持为空（显示「暂无防治方法」）。
     */
    @NonNull
    public static List<String> withLabelReminder(@NonNull List<String> chem, @NonNull String reminder) {
        if (chem.isEmpty()) return chem;
        for (String s : chem) {
            if (s.contains("标签")) return chem;
        }
        List<String> out = new ArrayList<>(chem);
        out.add(reminder);
        return out;
    }

    /** 用 sep 连接非空的部分 */
    @NonNull
    public static String join(@NonNull String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p.trim());
        }
        return sb.toString();
    }
}
