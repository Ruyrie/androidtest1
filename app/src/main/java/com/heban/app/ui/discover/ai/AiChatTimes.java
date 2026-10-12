package com.heban.app.ui.discover.ai;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

/** 历史会话的时间：今天 14:20 / 昨天 09:05 / 10月8日 / 2025年3月1日。 */
public final class AiChatTimes {
    private AiChatTimes() {
    }

    /**
     * @param today     「今天 %1$s」
     * @param yesterday 「昨天 %1$s」
     * @param thisYear  「%1$d月%2$d日」
     * @param otherYear 「%1$d年%2$d月%3$d日」
     */
    @NonNull
    public static String format(long epochMillis, long nowMillis, @NonNull ZoneId zone, @NonNull String today,
                                @NonNull String yesterday, @NonNull String thisYear, @NonNull String otherYear) {
        LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone);
        LocalDate now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone).toLocalDate();
        LocalDate d = t.toLocalDate();
        String hm = String.format(Locale.ROOT, "%02d:%02d", t.getHour(), t.getMinute());
        if (d.equals(now)) return String.format(Locale.ROOT, today, hm);
        if (d.equals(now.minusDays(1))) return String.format(Locale.ROOT, yesterday, hm);
        if (d.getYear() == now.getYear()) {
            return String.format(Locale.ROOT, thisYear, d.getMonthValue(), d.getDayOfMonth());
        }
        return String.format(Locale.ROOT, otherYear, d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }
}
