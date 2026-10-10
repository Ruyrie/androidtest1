package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.CropEntity;

/**
 * 「需关注」天气自动判断规则（纯 Java）：
 * <ul>
 *   <li>天气给出说明：用户手动取消后 3 天内不标记；已有非天气来源（识别结果等）的标记不覆盖；
 *       与当前天气标记相同则不改；否则标记，来源 weather；</li>
 *   <li>天气没有说明：只清除来源为 weather 的标记。</li>
 * </ul>
 */
public final class AttentionRules {
    public static final long DISMISS_QUIET_MS = 3L * 24 * 60 * 60 * 1000;

    public enum Action { KEEP, MARK, CLEAR }

    private AttentionRules() {
    }

    public static Action weather(@Nullable String currentNote, @Nullable String currentSource,
                                 long dismissedAt, @Nullable String weatherNote, long now) {
        boolean hasNote = currentNote != null && !currentNote.isEmpty();
        boolean fromWeather = CropEntity.ATTENTION_WEATHER.equals(currentSource);
        if (weatherNote != null && !weatherNote.trim().isEmpty()) {
            if (hasNote && !fromWeather) return Action.KEEP;            // 识别结果等其他来源优先
            if (dismissedAt > 0 && now - dismissedAt < DISMISS_QUIET_MS) return Action.KEEP;
            if (hasNote && weatherNote.trim().equals(currentNote)) return Action.KEEP;
            return Action.MARK;
        }
        return hasNote && fromWeather ? Action.CLEAR : Action.KEEP;
    }

    public static Action weather(CropEntity crop, @Nullable String weatherNote, long now) {
        return weather(crop.attentionNote, crop.attentionSource, crop.attentionDismissedAt, weatherNote, now);
    }
}
