package com.heban.app.data.model;

import java.time.YearMonth;

/** D10 结余趋势柱状图的一根柱子。 */
public final class MonthBalance {
    public final YearMonth month;
    public final long balanceCent;

    public MonthBalance(YearMonth month, long balanceCent) {
        this.month = month;
        this.balanceCent = balanceCent;
    }
}
