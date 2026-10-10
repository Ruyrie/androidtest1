package com.heban.app.data.repository;

/** 积分规则常量（技术准备文档表 12-1，与 P02 积分规则页一致）。 */
public final class PointsRules {
    private PointsRules() {
    }

    // 流水类型
    public static final String TYPE_CHECKIN = "CHECKIN";
    public static final String TYPE_CHECKIN_STREAK = "CHECKIN_STREAK";
    public static final String TYPE_POST = "POST";
    public static final String TYPE_ANSWER = "ANSWER";
    public static final String TYPE_ANSWER_ACCEPTED = "ANSWER_ACCEPTED";
    public static final String TYPE_FARM_TASK = "FARM_TASK";
    public static final String TYPE_LEDGER = "LEDGER";
    public static final String TYPE_PIN = "PIN";
    public static final String TYPE_REWARD_QUESTION = "REWARD_QUESTION";
    public static final String TYPE_REWARD_REFUND = "REWARD_REFUND";
    public static final String TYPE_REDEEM = "REDEEM";

    // 获得：积分 / 成长值 / 每日次数
    public static final int CHECKIN_POINTS = 10;
    public static final int CHECKIN_GROWTH = 5;
    public static final int STREAK_DAYS = 7;
    public static final int STREAK_BONUS_POINTS = 20;
    public static final int STREAK_BONUS_GROWTH = 10;
    public static final int POST_POINTS = 5;
    public static final int POST_GROWTH = 3;
    public static final int POST_DAILY = 2;
    public static final int ANSWER_POINTS = 10;
    public static final int ANSWER_GROWTH = 5;
    public static final int ANSWER_DAILY = 3;
    public static final int ACCEPTED_GROWTH = 20;
    public static final int FARM_TASK_POINTS = 5;
    public static final int FARM_TASK_GROWTH = 3;
    public static final int FARM_TASK_DAILY = 1;
    public static final int LEDGER_POINTS = 2;
    public static final int LEDGER_GROWTH = 1;
    public static final int LEDGER_DAILY = 1;
    /** 每天通过任务获得积分上限（悬赏积分不计入） */
    public static final int DAILY_TASK_CAP = 100;

    // 消耗
    public static final int PIN_COST = 100;
    public static final long PIN_DURATION_MS = 24L * 60 * 60 * 1000;
    public static final int REWARD_MIN = 10;
    public static final int REWARD_MAX = 100;
    public static final int REWARD_REFUND_DAYS = 7;

    /** 等级阈值（成长值）：Lv.1–6 */
    public static final int[] LEVEL_THRESHOLDS = {0, 100, 300, 800, 1500, 3000};

    public static int level(int growth) {
        int lv = 1;
        for (int i = 0; i < LEVEL_THRESHOLDS.length; i++) {
            if (growth >= LEVEL_THRESHOLDS[i]) lv = i + 1;
        }
        return lv;
    }

    /** 距下一级还差的成长值，满级为 0 */
    public static int nextLevelGap(int growth) {
        int lv = level(growth);
        return lv >= LEVEL_THRESHOLDS.length ? 0 : LEVEL_THRESHOLDS[lv] - growth;
    }

    /** Lv.4 起获得「种植能手」称号 */
    public static String levelTitle(int level) {
        return level >= 4 ? "种植能手" : "";
    }
}
