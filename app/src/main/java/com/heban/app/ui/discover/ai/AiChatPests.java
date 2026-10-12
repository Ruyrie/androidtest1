package com.heban.app.ui.discover.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.PestEntity;

import java.util.List;

/**
 * 回答下方的「霜霉病百科」：在回答中找出最先出现的本地百科条目名称（如「黄瓜霜霉病」），
 * 标签去掉开头的作物名（「黄瓜」）只留病名「霜霉病」。
 */
public final class AiChatPests {
    private AiChatPests() {
    }

    /** 回答中最先出现（同一位置取名称更长）的百科条目；没有时返回 null */
    @Nullable
    public static PestEntity match(@Nullable String answer, @Nullable List<PestEntity> pests) {
        if (answer == null || answer.isEmpty() || pests == null) return null;
        PestEntity best = null;
        int bestAt = Integer.MAX_VALUE;
        for (PestEntity p : pests) {
            if (p == null || p.name.trim().length() < 2) continue;
            int at = answer.indexOf(p.name.trim());
            if (at < 0) continue;
            if (at < bestAt || (at == bestAt && p.name.length() > best.name.length())) {
                best = p;
                bestAt = at;
            }
        }
        return best;
    }

    /** 「黄瓜霜霉病」→「霜霉病」：去掉开头的作物短名（cropTags），剩余不足 2 个字时保留全名 */
    @NonNull
    public static String shortName(@NonNull PestEntity p) {
        String name = p.name.trim();
        if (p.cropTags == null) return name;
        for (String tag : p.cropTags.split("[、,，\\s]+")) {
            String t = tag.trim();
            if (!t.isEmpty() && name.startsWith(t) && name.length() - t.length() >= 2) {
                return name.substring(t.length());
            }
        }
        return name;
    }
}
