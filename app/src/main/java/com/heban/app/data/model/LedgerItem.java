package com.heban.app.data.model;

import com.heban.app.data.local.entity.LedgerEntryEntity;

import java.util.Objects;

/** D10 流水列表中的一条：记账条目 + 关联作物与地块名。 */
public final class LedgerItem {
    public LedgerEntryEntity entry;
    public String cropName;
    public String plotName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LedgerItem)) return false;
        LedgerItem i = (LedgerItem) o;
        return entry != null && i.entry != null && entry.id == i.entry.id
                && entry.amountCent == i.entry.amountCent && Objects.equals(entry.note, i.entry.note)
                && Objects.equals(entry.category, i.entry.category) && entry.date == i.entry.date
                && Objects.equals(cropName, i.cropName) && Objects.equals(plotName, i.plotName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry == null ? 0 : entry.id);
    }
}
