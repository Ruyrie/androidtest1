package com.heban.app.data.repository;

import androidx.annotation.Nullable;

import com.heban.app.data.local.dao.PointsTotals;
import com.heban.app.data.local.entity.CheckinEntity;
import com.heban.app.data.model.CheckInState;
import com.heban.app.data.model.PointsSummary;
import com.heban.app.data.remote.BusinessException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分规则计算（技术准备文档 12.2 H05、表 12-1；纯 Java，便于单元测试）：
 * 每日次数、每日任务积分上限、签到连续天数与满 7 天奖励、等级概况、H05 签到格子。
 */
public final class PointsCalculator {
    public static final String ALREADY_CHECKED_IN = "ALREADY_CHECKED_IN";
    public static final String POINTS_NOT_ENOUGH = "POINTS_NOT_ENOUGH";

    /** P01 明细筛选 */
    public static final String FILTER_ALL = "all";
    public static final String FILTER_GAIN = "gain";
    public static final String FILTER_USE = "use";

    /** 计入「每天通过任务最多获得 100 积分」的流水类型；悬赏、采纳奖励、退回不计入 */
    public static final List<String> CAPPED_TYPES = Collections.unmodifiableList(Arrays.asList(
            PointsRules.TYPE_CHECKIN, PointsRules.TYPE_CHECKIN_STREAK, PointsRules.TYPE_POST,
            PointsRules.TYPE_ANSWER, PointsRules.TYPE_FARM_TASK, PointsRules.TYPE_LEDGER));

    /** 满 7 天奖励的明细标题（与 P02「连续签到满 7 天（当天额外）」一致） */
    public static final String STREAK_BONUS_TITLE = "连续签到满 7 天";

    private PointsCalculator() {
    }

    /** 一次奖励：积分与成长值 */
    public static final class Reward {
        public final int points;
        public final int growth;

        public Reward(int points, int growth) {
            this.points = points;
            this.growth = growth;
        }
    }

    // —— 每日任务（award） ——

    /** award 支持的类型每天可获得的次数；其他类型为 0 */
    public static int dailyLimit(String type) {
        switch (type) {
            case PointsRules.TYPE_POST:
                return PointsRules.POST_DAILY;
            case PointsRules.TYPE_ANSWER:
                return PointsRules.ANSWER_DAILY;
            case PointsRules.TYPE_FARM_TASK:
                return PointsRules.FARM_TASK_DAILY;
            case PointsRules.TYPE_LEDGER:
                return PointsRules.LEDGER_DAILY;
            default:
                return 0;
        }
    }

    /** 表 12-1 中某类任务一次的积分与成长值；award 不支持的类型抛出 IllegalArgumentException */
    public static Reward baseReward(String type) {
        switch (type) {
            case PointsRules.TYPE_POST:
                return new Reward(PointsRules.POST_POINTS, PointsRules.POST_GROWTH);
            case PointsRules.TYPE_ANSWER:
                return new Reward(PointsRules.ANSWER_POINTS, PointsRules.ANSWER_GROWTH);
            case PointsRules.TYPE_FARM_TASK:
                return new Reward(PointsRules.FARM_TASK_POINTS, PointsRules.FARM_TASK_GROWTH);
            case PointsRules.TYPE_LEDGER:
                return new Reward(PointsRules.LEDGER_POINTS, PointsRules.LEDGER_GROWTH);
            default:
                throw new IllegalArgumentException("Unsupported award type: " + type);
        }
    }

    /** 今天还能通过任务获得的积分（earnedToday 为今天 CAPPED_TYPES 已获得的积分） */
    public static int remainingCap(int earnedToday) {
        return Math.max(0, PointsRules.DAILY_TASK_CAP - earnedToday);
    }

    /**
     * 一次任务奖励：今天这类任务已达到次数时返回 null（不写流水）；否则积分不超过今天剩余的上限，
     * 成长值照常给（上限只限制积分）。
     *
     * @param countToday  今天这类流水已有的条数
     * @param earnedToday 今天 CAPPED_TYPES 已获得的积分
     */
    @Nullable
    public static Reward award(String type, int countToday, int earnedToday) {
        Reward base = baseReward(type);
        if (countToday >= dailyLimit(type)) return null;
        return new Reward(Math.min(base.points, remainingCap(earnedToday)), base.growth);
    }

    // —— 签到 ——

    /** 是否为满 7 天奖励日（第 7、14、21 … 天） */
    public static boolean isStreakBonusDay(int streak) {
        return streak > 0 && streak % PointsRules.STREAK_DAYS == 0;
    }

    /** 连续第 streak 天签到可得的积分（第 7 天为 10 + 20 = 30），不考虑每日上限 */
    public static int checkInPoints(int streak) {
        return PointsRules.CHECKIN_POINTS + (isStreakBonusDay(streak) ? PointsRules.STREAK_BONUS_POINTS : 0);
    }

    /** 今天签到后的连续天数：昨天签过为昨天的连续天数 + 1，否则从 1 开始 */
    public static int nextStreak(@Nullable CheckinEntity yesterday) {
        return yesterday == null ? 1 : yesterday.streak + 1;
    }

    /** 签到流水标题：「每日签到 · 连续第 5 天」 */
    public static String checkInTitle(int streak) {
        return "每日签到 · 连续第 " + streak + " 天";
    }

    /** 一次签到要写入的内容 */
    public static final class CheckInPlan {
        public final int streak;
        /** 每日签到（+10 / +5） */
        public final Reward daily;
        /** 满 7 天奖励（+20 / +10），不是奖励日为 null */
        @Nullable
        public final Reward bonus;

        CheckInPlan(int streak, Reward daily, @Nullable Reward bonus) {
            this.streak = streak;
            this.daily = daily;
            this.bonus = bonus;
        }

        /** 当天签到获得的积分合计（写入 checkin.points） */
        public int totalPoints() {
            return daily.points + (bonus == null ? 0 : bonus.points);
        }

        public int totalGrowth() {
            return daily.growth + (bonus == null ? 0 : bonus.growth);
        }
    }

    /**
     * 今天签到：已签过抛出 BusinessException(ALREADY_CHECKED_IN, "今天已签到")；连续天数接着昨天计算；
     * 第 7 天加满 7 天奖励。签到积分同样计入每日 100 上限（按现行规则每天任务最多 77 积分，实际不会触顶）。
     *
     * @param checkedToday 今天是否已有签到记录
     * @param yesterday    昨天的签到记录，没有为 null
     * @param earnedToday  今天 CAPPED_TYPES 已获得的积分
     */
    public static CheckInPlan planCheckIn(boolean checkedToday, @Nullable CheckinEntity yesterday, int earnedToday) {
        if (checkedToday) throw new BusinessException(ALREADY_CHECKED_IN, "今天已签到");
        int streak = nextStreak(yesterday);
        int remaining = remainingCap(earnedToday);
        Reward daily = new Reward(Math.min(PointsRules.CHECKIN_POINTS, remaining), PointsRules.CHECKIN_GROWTH);
        Reward bonus = null;
        if (isStreakBonusDay(streak)) {
            bonus = new Reward(Math.min(PointsRules.STREAK_BONUS_POINTS, remaining - daily.points),
                    PointsRules.STREAK_BONUS_GROWTH);
        }
        return new CheckInPlan(streak, daily, bonus);
    }

    /**
     * H05 / M01 签到状态：以今天结尾的 7 天格子（原型「4日 … 今天」）。
     * 已签的格子显示当天实际获得的积分；今天未签显示可得积分（10，第 7 天 30）；之前漏签的格子为 0。
     *
     * @param recent 最近的签到记录（至少包含今天往前 7 天）
     */
    public static CheckInState checkInState(List<CheckinEntity> recent, LocalDate today) {
        Map<String, CheckinEntity> byDate = new HashMap<>();
        for (CheckinEntity c : recent) byDate.put(c.date, c);
        CheckinEntity todayRow = byDate.get(today.toString());
        CheckinEntity yesterday = byDate.get(today.minusDays(1).toString());

        CheckInState s = new CheckInState();
        s.checkedToday = todayRow != null;
        s.streak = todayRow != null ? todayRow.streak : yesterday != null ? yesterday.streak : 0;
        s.todayPoints = todayRow != null ? todayRow.points : checkInPoints(s.streak + 1);
        for (int i = PointsRules.STREAK_DAYS - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            CheckinEntity row = byDate.get(date.toString());
            CheckInState.Day d = new CheckInState.Day();
            d.date = date;
            d.today = i == 0;
            d.checked = row != null;
            d.points = row != null ? row.points : d.today ? s.todayPoints : 0;
            s.week.add(d);
        }
        return s;
    }

    // —— 概况 ——

    /** 余额、成长值 → 等级、称号、距下一级的成长值，以及本月获得 / 消耗 */
    public static PointsSummary summary(int balance, int growth, int monthGain, int monthSpend) {
        PointsSummary s = new PointsSummary();
        s.balance = balance;
        s.growth = growth;
        s.level = PointsRules.level(growth);
        s.levelTitle = PointsRules.levelTitle(s.level);
        s.nextLevelGap = PointsRules.nextLevelGap(growth);
        s.monthGain = monthGain;
        s.monthSpend = monthSpend;
        return s;
    }

    public static PointsSummary summary(PointsTotals t) {
        return summary(t.balance, t.growth, t.monthGain, t.monthSpend);
    }

    /** P01 筛选：all / gain / use，其他值按 all 处理 */
    public static String normalizeFilter(@Nullable String filter) {
        return FILTER_GAIN.equals(filter) || FILTER_USE.equals(filter) ? filter : FILTER_ALL;
    }
}
