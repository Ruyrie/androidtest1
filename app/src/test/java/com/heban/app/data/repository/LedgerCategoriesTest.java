package com.heban.app.data.repository;

import static org.junit.Assert.assertEquals;

import com.heban.app.data.local.entity.FarmRecordEntity;
import com.heban.app.data.local.entity.LedgerEntryEntity;

import org.junit.Test;

public class LedgerCategoriesTest {

    @Test
    public void categoryByRecordType() {
        assertEquals("化肥", LedgerCategories.forRecordType("FERTILIZE"));
        assertEquals("农药", LedgerCategories.forRecordType("SPRAY"));
        assertEquals("水电灌溉", LedgerCategories.forRecordType("IRRIGATE"));
        assertEquals("人工", LedgerCategories.forRecordType("PRUNE"));
        assertEquals("其他", LedgerCategories.forRecordType("HARVEST"));
        assertEquals("其他", LedgerCategories.forRecordType("OTHER"));
        assertEquals("其他", LedgerCategories.forRecordType("???"));
        assertEquals("其他", LedgerCategories.forRecordType(null));
    }

    @Test
    public void note() {
        assertEquals("施肥 · 硫酸钾", LedgerCategories.noteFor("FERTILIZE", " 硫酸钾 "));
        assertEquals("用药", LedgerCategories.noteFor("SPRAY", ""));
        assertEquals("灌溉", LedgerCategories.noteFor("IRRIGATE", null));
    }

    @Test
    public void entryFromRecord() {
        FarmRecordEntity r = new FarmRecordEntity();
        r.id = 7;
        r.userId = 3;
        r.cropId = 5;
        r.type = "FERTILIZE";
        r.material = "硫酸钾";
        r.costCent = 6000;
        r.date = 1_790_000_000_000L;
        LedgerEntryEntity e = LedgerCategories.entryFor(r);
        assertEquals(LedgerEntryEntity.TYPE_OUT, e.type);
        assertEquals("化肥", e.category);
        assertEquals(6000, e.amountCent);
        assertEquals(Long.valueOf(5), e.cropId);
        assertEquals(Long.valueOf(7), e.recordId);
        assertEquals(r.date, e.date);
        assertEquals("施肥 · 硫酸钾", e.note);
        assertEquals(LedgerEntryEntity.SOURCE_RECORD, e.source);
        assertEquals(3, e.userId);
    }
}
