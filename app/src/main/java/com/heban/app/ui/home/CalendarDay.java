package com.heban.app.ui.home;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/** H03 月历的一个格子；date 为 null 表示月初前的空格。 */
public final class CalendarDay {
    @Nullable public final LocalDate date;
    public final boolean today;
    public final boolean selected;
    /** 节气名称，没有为 null */
    @Nullable public final String term;
    /** 当天各配色的农事圆点（见 CalendarTypeStyle） */
    public final boolean dotManage;
    public final boolean dotIrrigate;
    public final boolean dotPest;

    public CalendarDay(@Nullable LocalDate date, boolean today, boolean selected, @Nullable String term,
                       boolean dotManage, boolean dotIrrigate, boolean dotPest) {
        this.date = date;
        this.today = today;
        this.selected = selected;
        this.term = term;
        this.dotManage = dotManage;
        this.dotIrrigate = dotIrrigate;
        this.dotPest = dotPest;
    }

    static CalendarDay blank() {
        return new CalendarDay(null, false, false, null, false, false, false);
    }

    public boolean isBlank() {
        return date == null;
    }

    /** 日期数字，空格为空字符串 */
    public String dayText() {
        return date == null ? "" : String.valueOf(date.getDayOfMonth());
    }

    public boolean hasTerm() {
        return term != null;
    }

    public boolean hasDots() {
        return dotManage || dotIrrigate || dotPest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalendarDay)) return false;
        CalendarDay d = (CalendarDay) o;
        return today == d.today && selected == d.selected && dotManage == d.dotManage
                && dotIrrigate == d.dotIrrigate && dotPest == d.dotPest
                && Objects.equals(date, d.date) && Objects.equals(term, d.term);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, today, selected, term, dotManage, dotIrrigate, dotPest);
    }
}
