package com.heban.app.ui.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.R;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

public class AddCropHarvestTest {

    @Test
    public void expectedHarvest_addsGrowthDays() {
        // 原型：2026年10月10日定植，按 150 天计 → 2027年3月上旬
        LocalDate h = AddCropHarvest.expectedHarvest(LocalDate.of(2026, 10, 10), 150);
        assertEquals(LocalDate.of(2027, 3, 9), h);
        assertEquals(R.string.add_crop_period_early, AddCropHarvest.periodRes(h));
    }

    @Test
    public void expectedHarvest_unknownDays_isNull() {
        assertNull(AddCropHarvest.expectedHarvest(LocalDate.of(2026, 10, 10), 0));
        assertNull(AddCropHarvest.expectedHarvest(null, 90));
    }

    @Test
    public void periodRes_splitsMonthIntoThree() {
        assertEquals(R.string.add_crop_period_early, AddCropHarvest.periodRes(LocalDate.of(2026, 3, 1)));
        assertEquals(R.string.add_crop_period_early, AddCropHarvest.periodRes(LocalDate.of(2026, 3, 10)));
        assertEquals(R.string.add_crop_period_middle, AddCropHarvest.periodRes(LocalDate.of(2026, 3, 11)));
        assertEquals(R.string.add_crop_period_middle, AddCropHarvest.periodRes(LocalDate.of(2026, 3, 20)));
        assertEquals(R.string.add_crop_period_late, AddCropHarvest.periodRes(LocalDate.of(2026, 3, 21)));
        assertEquals(R.string.add_crop_period_late, AddCropHarvest.periodRes(LocalDate.of(2026, 2, 28)));
    }

    @Test
    public void pickerSelection_roundTrips() {
        LocalDate d = LocalDate.of(2026, 10, 10);
        long utc = AddCropHarvest.toPickerSelection(d);
        assertEquals(1791590400000L, utc);
        assertEquals(d, AddCropHarvest.fromPickerSelection(utc));
    }

    @Test
    public void startOfDay_usesZone() {
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        long ms = AddCropHarvest.startOfDayMillis(LocalDate.of(2026, 10, 10), shanghai);
        assertEquals(1791590400000L - 8 * 3600_000L, ms);
        assertEquals(LocalDate.of(2026, 10, 10), AddCropHarvest.toLocalDate(ms, shanghai));
    }

    @Test
    public void parseDecimal() {
        assertNull(AddCropHarvest.parseDecimal(null));
        assertNull(AddCropHarvest.parseDecimal("  "));
        assertEquals(0.3, AddCropHarvest.parseDecimal("0.3"), 1e-9);
        assertEquals(0.5, AddCropHarvest.parseDecimal(".5"), 1e-9);
        assertEquals(12.0, AddCropHarvest.parseDecimal("12."), 1e-9);
        assertEquals(0.0, AddCropHarvest.parseDecimal("0"), 1e-9);
        assertTrue(AddCropHarvest.parseDecimal(".").isNaN());
        assertTrue(AddCropHarvest.parseDecimal("1.2.3").isNaN());
        assertTrue(AddCropHarvest.parseDecimal("-1").isNaN());
    }

    @Test
    public void formatDecimal_stripsZeros() {
        assertEquals("0.3", AddCropHarvest.formatDecimal(0.3));
        assertEquals("2", AddCropHarvest.formatDecimal(2.0));
        assertEquals("", AddCropHarvest.formatDecimal(0));
    }
}
