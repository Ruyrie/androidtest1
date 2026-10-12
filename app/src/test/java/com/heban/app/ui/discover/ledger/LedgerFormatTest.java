package com.heban.app.ui.discover.ledger;

import static org.junit.Assert.assertEquals;

import com.heban.app.data.local.entity.LedgerEntryEntity;
import com.heban.app.data.model.LedgerItem;

import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;

/** D10 / D11 文字格式与原型一致。 */
public class LedgerFormatTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    @Test
    public void money() {
        assertEquals("+¥3,460", LedgerFormat.signedMoney(346000));
        assertEquals("−¥280", LedgerFormat.signedMoney(-28000));
        assertEquals("¥0", LedgerFormat.signedMoney(0));
        assertEquals("¥5,200", LedgerFormat.money(520000));
        assertEquals("¥12.5", LedgerFormat.money(1250));
        assertEquals("¥12.05", LedgerFormat.money(1205));
        assertEquals("+960", LedgerFormat.entryAmount(LedgerEntryEntity.TYPE_IN, 96000));
        assertEquals("−60", LedgerFormat.entryAmount(LedgerEntryEntity.TYPE_OUT, 6000));
    }

    @Test
    public void months() {
        assertEquals("2026年10月", LedgerFormat.monthTitle(YearMonth.of(2026, 10)));
        assertEquals("5月", LedgerFormat.monthShort(YearMonth.of(2026, 5)));
    }

    @Test
    public void days() {
        assertEquals("今天 · 10月10日", LedgerFormat.dayTitle(TODAY, TODAY));
        assertEquals("昨天 · 10月9日", LedgerFormat.dayTitle(TODAY.minusDays(1), TODAY));
        assertEquals("周四 · 10月8日", LedgerFormat.dayTitle(TODAY.minusDays(2), TODAY));
        assertEquals("收 960 · 支 60", LedgerFormat.daySummary(96000, 6000));
        assertEquals("支 60", LedgerFormat.daySummary(0, 6000));
        assertEquals("收 960", LedgerFormat.daySummary(96000, 0));
    }

    @Test
    public void items() {
        LedgerItem sale = LedgerFakes.item(LedgerFakes.entry(1, LedgerEntryEntity.TYPE_IN, "农产品销售", 96000, TODAY,
                "樱桃番茄 120 斤", LedgerEntryEntity.SOURCE_MANUAL), "番茄", "1号大棚");
        assertEquals("樱桃番茄 120 斤", LedgerFormat.itemTitle(sale));
        assertEquals("销售 · 番茄 · 1号大棚", LedgerFormat.itemSub(sale));

        LedgerItem record = LedgerFakes.item(LedgerFakes.entry(2, LedgerEntryEntity.TYPE_OUT, "化肥", 6000, TODAY,
                "硫酸钾 5 kg", LedgerEntryEntity.SOURCE_RECORD), "番茄", "1号大棚");
        assertEquals("化肥 · 番茄 · 来自农事记录", LedgerFormat.itemSub(record));

        LedgerItem bare = LedgerFakes.item(LedgerFakes.entry(3, LedgerEntryEntity.TYPE_OUT, "地租", 50000, TODAY,
                null, LedgerEntryEntity.SOURCE_MANUAL), null, null);
        assertEquals("地租", LedgerFormat.itemTitle(bare));
        assertEquals("地租", LedgerFormat.itemSub(bare));
    }

    @Test
    public void addEntryTexts() {
        assertEquals("支出 · 化肥", LedgerFormat.entryHeader(LedgerEntryEntity.TYPE_OUT, "化肥"));
        assertEquals("收入 · 农产品销售", LedgerFormat.entryHeader(LedgerEntryEntity.TYPE_IN, "农产品销售"));
        assertEquals("−¥60", LedgerFormat.amountDisplay(LedgerEntryEntity.TYPE_OUT, "60"));
        assertEquals("+¥0.", LedgerFormat.amountDisplay(LedgerEntryEntity.TYPE_IN, "0."));
        assertEquals("今天 10/10", LedgerFormat.dateChip(TODAY, TODAY));
        assertEquals("昨天 10/09", LedgerFormat.dateChip(TODAY.minusDays(1), TODAY));
        assertEquals("10/01", LedgerFormat.dateChip(LocalDate.of(2026, 10, 1), TODAY));
        assertEquals("2025/12/31", LedgerFormat.dateChip(LocalDate.of(2025, 12, 31), TODAY));
        assertEquals("番茄 · 1号大棚", LedgerFormat.cropLabel("番茄", "1号大棚"));
        assertEquals("番茄", LedgerFormat.cropLabel("番茄", null));
    }
}
