package com.heban.app.ui.discover.ledger;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.model.LedgerDay;
import com.heban.app.data.model.LedgerItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * D10 明细列表的一行：按天分组的标题行（「今天 · 10月10日」「收 960 · 支 60」），或一条流水。
 * 同一天的流水放在一张圆角卡片里，first / last 决定卡片的上下圆角与分隔线。
 */
public final class LedgerRow {
    public static final int TYPE_DAY = 0;
    public static final int TYPE_ITEM = 1;

    public final int type;
    /** 稳定 id：标题行为 −epochDay − 1，流水为条目 id */
    public final long key;
    @NonNull public final String title;
    @NonNull public final String sub;
    /** 标题行右侧合计；流水右侧金额 */
    @NonNull public final String amount;
    public final boolean income;
    /** 来自农事记录（不能在这里编辑 / 删除） */
    public final boolean fromRecord;
    public final long entryId;
    @DrawableRes public final int icon;
    public final boolean first;
    public final boolean last;

    private LedgerRow(int type, long key, @NonNull String title, @NonNull String sub, @NonNull String amount,
                      boolean income, boolean fromRecord, long entryId, int icon, boolean first, boolean last) {
        this.type = type;
        this.key = key;
        this.title = title;
        this.sub = sub;
        this.amount = amount;
        this.income = income;
        this.fromRecord = fromRecord;
        this.entryId = entryId;
        this.icon = icon;
        this.first = first;
        this.last = last;
    }

    /** 按天分组的流水（新 → 旧）→ 列表行 */
    @NonNull
    public static List<LedgerRow> from(@Nullable List<LedgerDay> days, @NonNull LocalDate today) {
        if (days == null || days.isEmpty()) return Collections.emptyList();
        List<LedgerRow> rows = new ArrayList<>();
        for (LedgerDay d : days) {
            if (d == null || d.date == null || d.items == null) continue;
            List<LedgerItem> items = new ArrayList<>();
            for (LedgerItem item : d.items) if (item != null && item.entry != null) items.add(item);
            if (items.isEmpty()) continue;
            rows.add(new LedgerRow(TYPE_DAY, -d.date.toEpochDay() - 1, LedgerFormat.dayTitle(d.date, today), "",
                    LedgerFormat.daySummary(d.incomeCent, d.expenseCent), false, false, 0, 0, false, false));
            int n = items.size();
            for (int i = 0; i < n; i++) {
                LedgerItem item = items.get(i);
                LedgerEntryEntity e = item.entry;
                boolean income = LedgerFormat.isIncome(e.type);
                rows.add(new LedgerRow(TYPE_ITEM, e.id, LedgerFormat.itemTitle(item), LedgerFormat.itemSub(item),
                        LedgerFormat.entryAmount(e.type, e.amountCent), income, LedgerFormat.isFromRecord(e), e.id,
                        LedgerCategoryIcons.icon(e.type, e.category), i == 0, i == n - 1));
            }
        }
        return rows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LedgerRow)) return false;
        LedgerRow r = (LedgerRow) o;
        return type == r.type && key == r.key && income == r.income && fromRecord == r.fromRecord
                && entryId == r.entryId && icon == r.icon && first == r.first && last == r.last
                && title.equals(r.title) && sub.equals(r.sub) && amount.equals(r.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, key, title, amount);
    }
}
