package com.heban.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.PestEntity;

import java.util.List;

/**
 * 百科条目在各页面上的显示文字（纯 Java），与原型一致：
 * <ul>
 *   <li>D04 列表副标题 {@link #subtitle}：「黄瓜 · 卵菌病害」「瓜类、草莓 · 真菌病害」；</li>
 *   <li>D01 本季高发卡片 {@link #shortName} + {@link #seasonCrops}：「霜霉病」「黄瓜 · 白菜」；</li>
 *   <li>D04 本季高发横幅：标题 {@link #shortName}「霜霉病」，说明直接用 tip「黄瓜、白菜等 · 秋季低温高湿易发」。</li>
 * </ul>
 * tip 的格式固定为「作物范围 · 一句话原因」。
 */
public final class PestTexts {
    private static final String DOT = " · ";

    private PestTexts() {
    }

    /** 作物标签 + 细分类别，如「黄瓜 · 卵菌病害」 */
    public static String subtitle(@NonNull PestEntity p) {
        String crops = p.cropTags == null ? "" : p.cropTags.trim();
        String cat = p.category == null ? "" : p.category.trim();
        if (crops.isEmpty()) return cat;
        if (cat.isEmpty()) return crops;
        return crops + DOT + cat;
    }

    /** 去掉名称开头的作物名，如「黄瓜霜霉病」→「霜霉病」「番茄灰霉病」→「灰霉病」；剩余不足 2 个字时保留原名 */
    public static String shortName(@NonNull PestEntity p) {
        String name = p.name;
        String best = name;
        for (String tag : PestQueries.splitList(p.cropTags)) {
            if (name.startsWith(tag)) {
                String rest = name.substring(tag.length());
                if (rest.length() >= PestQueries.MIN_MATCH_LENGTH && rest.length() < best.length()) best = rest;
            }
        }
        return best;
    }

    /** tip 中「 · 」前的作物范围，去掉末尾的「等」，作物之间用「 · 」分隔，如「黄瓜 · 白菜」；没有 tip 时用作物标签 */
    public static String seasonCrops(@NonNull PestEntity p) {
        String scope = tipScope(p.tip);
        if (scope == null) scope = p.cropTags == null ? "" : p.cropTags;
        if (scope.endsWith("等")) scope = scope.substring(0, scope.length() - 1);
        List<String> parts = PestQueries.splitList(scope);
        return String.join(DOT, parts);
    }

    @Nullable
    static String tipScope(@Nullable String tip) {
        if (tip == null) return null;
        int i = tip.indexOf(DOT);
        if (i <= 0) return null;
        return tip.substring(0, i).trim();
    }
}
