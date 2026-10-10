package com.heban.app.data.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** D10 按天分组：「今天 · 10月10日  收 960 · 支 60」。 */
public final class LedgerDay {
    public LocalDate date;
    public long incomeCent;
    public long expenseCent;
    public List<LedgerItem> items = new ArrayList<>();
}
