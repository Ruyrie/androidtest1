package com.heban.app.data.model;

/** D10 顶部：本月收入、支出、结余（分）。 */
public final class LedgerMonthSummary {
    public long incomeCent;
    public long expenseCent;

    public long balanceCent() {
        return incomeCent - expenseCent;
    }
}
