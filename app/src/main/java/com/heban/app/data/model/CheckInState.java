package com.heban.app.data.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** H05 / M01 签到状态。 */
public final class CheckInState {
    public boolean checkedToday;
    /** 当前连续天数（今天未签到时为截至昨天的连续天数） */
    public int streak;
    /** 今天签到可得 / 已得的积分（第 7 天为 30） */
    public int todayPoints;
    /** 最近 7 天（含今天）的签到格子 */
    public List<Day> week = new ArrayList<>();

    public static final class Day {
        public LocalDate date;
        public boolean checked;
        public boolean today;
        /** 这一天签到的积分（已签）或可得积分（未来 / 今天未签） */
        public int points;
    }
}
