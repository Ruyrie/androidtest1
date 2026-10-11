package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.heban.app.data.local.entity.CheckinEntity;
import com.heban.app.data.model.CheckInState;
import com.heban.app.data.model.PointsSummary;
import com.heban.app.data.remote.BusinessException;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 技术准备文档表 13-1 PointsRepository：同日重复签到、连续天数中断、第 7 天奖励、每日 100 上限、悬赏不计入上限。 */
public class PointsCalculatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private static CheckinEntity checkin(LocalDate date, int streak, int points) {
        CheckinEntity c = new CheckinEntity();
        c.userId = 1;
        c.date = date.toString();
        c.streak = streak;
        c.points = points;
        return c;
    }

    // —— 签到 ——

    @Test
    public void sameDayDoubleCheckInIsRejected() {
        try {
            PointsCalculator.planCheckIn(true, null, 0);
            fail("expected ALREADY_CHECKED_IN");
        } catch (BusinessException e) {
            assertEquals(PointsCalculator.ALREADY_CHECKED_IN, e.code);
            assertEquals("今天已签到", e.getMessage());
        }
    }

    @Test
    public void firstCheckInStartsStreakAtOne() {
        PointsCalculator.CheckInPlan plan = PointsCalculator.planCheckIn(false, null, 0);
        assertEquals(1, plan.streak);
        assertEquals(10, plan.totalPoints());
        assertEquals(5, plan.totalGrowth());
        assertNull(plan.bonus);
        assertEquals("每日签到 · 连续第 1 天", PointsCalculator.checkInTitle(plan.streak));
    }

    @Test
    public void streakContinuesFromYesterday() {
        PointsCalculator.CheckInPlan plan = PointsCalculator.planCheckIn(false,
                checkin(TODAY.minusDays(1), 4, 10), 0);
        assertEquals(5, plan.streak);
        assertEquals("每日签到 · 连续第 5 天", PointsCalculator.checkInTitle(plan.streak));
        assertEquals(10, plan.totalPoints());
    }

    @Test
    public void streakBreaksWithoutYesterday() {
        // 昨天没有签到（repository 只查询昨天那一行），从 1 重新开始
        assertEquals(1, PointsCalculator.nextStreak(null));
        assertEquals(1, PointsCalculator.planCheckIn(false, null, 0).streak);
    }

    @Test
    public void seventhDayAddsStreakBonus() {
        PointsCalculator.CheckInPlan plan = PointsCalculator.planCheckIn(false,
                checkin(TODAY.minusDays(1), 6, 10), 0);
        assertEquals(7, plan.streak);
        assertNotNull(plan.bonus);
        assertEquals(10, plan.daily.points);
        assertEquals(5, plan.daily.growth);
        assertEquals(20, plan.bonus.points);
        assertEquals(10, plan.bonus.growth);
        assertEquals(30, plan.totalPoints());
        assertEquals(15, plan.totalGrowth());
    }

    @Test
    public void bonusRepeatsEverySevenDays() {
        assertFalse(PointsCalculator.isStreakBonusDay(0));
        assertFalse(PointsCalculator.isStreakBonusDay(6));
        assertTrue(PointsCalculator.isStreakBonusDay(7));
        assertFalse(PointsCalculator.isStreakBonusDay(8));
        assertTrue(PointsCalculator.isStreakBonusDay(14));
        assertEquals(10, PointsCalculator.checkInPoints(13));
        assertEquals(30, PointsCalculator.checkInPoints(14));
    }

    @Test
    public void checkInRespectsDailyCap() {
        // 今天任务已得 95：签到只能再得 5，成长值照常
        PointsCalculator.CheckInPlan plan = PointsCalculator.planCheckIn(false,
                checkin(TODAY.minusDays(1), 6, 10), 95);
        assertEquals(5, plan.daily.points);
        assertEquals(0, plan.bonus.points);
        assertEquals(5, plan.totalPoints());
        assertEquals(15, plan.totalGrowth());
    }

    // —— 每日任务 ——

    @Test
    public void perTypeDailyCounts() {
        assertEquals(2, PointsCalculator.dailyLimit(PointsRules.TYPE_POST));
        assertEquals(3, PointsCalculator.dailyLimit(PointsRules.TYPE_ANSWER));
        assertEquals(1, PointsCalculator.dailyLimit(PointsRules.TYPE_FARM_TASK));
        assertEquals(1, PointsCalculator.dailyLimit(PointsRules.TYPE_LEDGER));

        assertEquals(5, PointsCalculator.award(PointsRules.TYPE_POST, 0, 0).points);
        assertEquals(3, PointsCalculator.award(PointsRules.TYPE_POST, 1, 0).growth);
        assertNull(PointsCalculator.award(PointsRules.TYPE_POST, 2, 0));

        assertEquals(10, PointsCalculator.award(PointsRules.TYPE_ANSWER, 2, 0).points);
        assertNull(PointsCalculator.award(PointsRules.TYPE_ANSWER, 3, 0));

        PointsCalculator.Reward task = PointsCalculator.award(PointsRules.TYPE_FARM_TASK, 0, 0);
        assertEquals(5, task.points);
        assertEquals(3, task.growth);
        assertNull(PointsCalculator.award(PointsRules.TYPE_FARM_TASK, 1, 0));

        PointsCalculator.Reward ledger = PointsCalculator.award(PointsRules.TYPE_LEDGER, 0, 0);
        assertEquals(2, ledger.points);
        assertEquals(1, ledger.growth);
        assertNull(PointsCalculator.award(PointsRules.TYPE_LEDGER, 1, 0));
    }

    @Test
    public void dailyCapOfOneHundred() {
        assertEquals(100, PointsCalculator.remainingCap(0));
        assertEquals(2, PointsCalculator.remainingCap(98));
        assertEquals(0, PointsCalculator.remainingCap(100));
        assertEquals(0, PointsCalculator.remainingCap(130));

        // 只补到上限
        PointsCalculator.Reward partial = PointsCalculator.award(PointsRules.TYPE_ANSWER, 0, 96);
        assertEquals(4, partial.points);
        assertEquals(5, partial.growth);

        // 已到上限：积分为 0，成长值照常（仍记一次，H05 显示已完成）
        PointsCalculator.Reward none = PointsCalculator.award(PointsRules.TYPE_FARM_TASK, 0, 100);
        assertEquals(0, none.points);
        assertEquals(3, none.growth);
    }

    @Test
    public void rewardPointsAreNotCountedInCap() {
        assertTrue(PointsCalculator.CAPPED_TYPES.containsAll(Arrays.asList(
                PointsRules.TYPE_CHECKIN, PointsRules.TYPE_CHECKIN_STREAK, PointsRules.TYPE_POST,
                PointsRules.TYPE_ANSWER, PointsRules.TYPE_FARM_TASK, PointsRules.TYPE_LEDGER)));
        assertFalse(PointsCalculator.CAPPED_TYPES.contains(PointsRules.TYPE_ANSWER_ACCEPTED));
        assertFalse(PointsCalculator.CAPPED_TYPES.contains(PointsRules.TYPE_REWARD_REFUND));
        assertFalse(PointsCalculator.CAPPED_TYPES.contains(PointsRules.TYPE_REWARD_QUESTION));
        assertFalse(PointsCalculator.CAPPED_TYPES.contains(PointsRules.TYPE_PIN));
        assertFalse(PointsCalculator.CAPPED_TYPES.contains(PointsRules.TYPE_REDEEM));
    }

    @Test(expected = IllegalArgumentException.class)
    public void awardRejectsNonTaskTypes() {
        PointsCalculator.award(PointsRules.TYPE_ANSWER_ACCEPTED, 0, 0);
    }

    // —— 等级 ——

    @Test
    public void levelThresholdsAndGap() {
        assertSummary(0, 1, 100, "");
        assertSummary(99, 1, 1, "");
        assertSummary(100, 2, 200, "");
        assertSummary(299, 2, 1, "");
        assertSummary(300, 3, 500, "");
        assertSummary(799, 3, 1, "");
        assertSummary(800, 4, 700, "种植能手");
        assertSummary(1280, 4, 220, "种植能手");   // 原型：距 Lv.5 还差 220 成长值
        assertSummary(1500, 5, 1500, "种植能手");
        assertSummary(3000, 6, 0, "种植能手");
        assertSummary(9999, 6, 0, "种植能手");
    }

    private static void assertSummary(int growth, int level, int gap, String title) {
        PointsSummary s = PointsCalculator.summary(1280, growth, 157, 520);
        assertEquals(level, s.level);
        assertEquals(gap, s.nextLevelGap);
        assertEquals(title, s.levelTitle);
        assertEquals(1280, s.balance);
        assertEquals(growth, s.growth);
        assertEquals(157, s.monthGain);
        assertEquals(520, s.monthSpend);
    }

    // —— H05 签到格子 ——

    @Test
    public void weekBeforeCheckInMatchesPrototype() {
        // 原型：4日–9日已签（连续 6 天），今天未签，今天格子 +30
        List<CheckinEntity> rows = new ArrayList<>();
        for (int i = 1; i <= 6; i++) rows.add(checkin(TODAY.minusDays(i), 7 - i, 10));
        CheckInState s = PointsCalculator.checkInState(rows, TODAY);
        assertFalse(s.checkedToday);
        assertEquals(6, s.streak);
        assertEquals(30, s.todayPoints);
        assertEquals(7, s.week.size());
        assertEquals(LocalDate.of(2026, 10, 4), s.week.get(0).date);
        assertEquals(TODAY, s.week.get(6).date);
        for (int i = 0; i < 6; i++) {
            assertTrue(s.week.get(i).checked);
            assertFalse(s.week.get(i).today);
            assertEquals(10, s.week.get(i).points);
        }
        assertFalse(s.week.get(6).checked);
        assertTrue(s.week.get(6).today);
        assertEquals(30, s.week.get(6).points);
    }

    @Test
    public void weekAfterCheckInShowsStoredPoints() {
        List<CheckinEntity> rows = new ArrayList<>();
        rows.add(checkin(TODAY, 7, 30));
        for (int i = 1; i <= 6; i++) rows.add(checkin(TODAY.minusDays(i), 7 - i, 10));
        CheckInState s = PointsCalculator.checkInState(rows, TODAY);
        assertTrue(s.checkedToday);
        assertEquals(7, s.streak);
        assertEquals(30, s.todayPoints);
        assertTrue(s.week.get(6).checked);
        assertEquals(30, s.week.get(6).points);
    }

    @Test
    public void brokenStreakResetsAndMissedDaysAreEmpty() {
        // 前天签到过，昨天漏签：连续天数为 0，今天可得 10，漏签的格子为 0
        List<CheckinEntity> rows = Collections.singletonList(checkin(TODAY.minusDays(2), 3, 10));
        CheckInState s = PointsCalculator.checkInState(rows, TODAY);
        assertFalse(s.checkedToday);
        assertEquals(0, s.streak);
        assertEquals(10, s.todayPoints);
        assertTrue(s.week.get(4).checked);
        assertEquals(10, s.week.get(4).points);
        assertFalse(s.week.get(5).checked);
        assertEquals(0, s.week.get(5).points);
        assertEquals(10, s.week.get(6).points);
    }

    @Test
    public void emptyStateForNewAccount() {
        CheckInState s = PointsCalculator.checkInState(Collections.emptyList(), TODAY);
        assertFalse(s.checkedToday);
        assertEquals(0, s.streak);
        assertEquals(10, s.todayPoints);
        assertEquals(7, s.week.size());
        assertTrue(s.week.get(6).today);
    }

    @Test
    public void filterNormalization() {
        assertEquals("gain", PointsCalculator.normalizeFilter("gain"));
        assertEquals("use", PointsCalculator.normalizeFilter("use"));
        assertEquals("all", PointsCalculator.normalizeFilter("all"));
        assertEquals("all", PointsCalculator.normalizeFilter(null));
        assertEquals("all", PointsCalculator.normalizeFilter("x"));
    }
}
