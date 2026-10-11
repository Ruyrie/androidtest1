package com.heban.app.ui.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.heban.app.R;
import com.heban.app.data.model.FarmType;

import org.junit.Test;

public class AddRecordMoneyTest {

    @Test
    public void parseCents_valid() {
        assertEquals(0, AddRecordMoney.parseCents(null));
        assertEquals(0, AddRecordMoney.parseCents(""));
        assertEquals(0, AddRecordMoney.parseCents(" "));
        assertEquals(6000, AddRecordMoney.parseCents("60"));
        assertEquals(6050, AddRecordMoney.parseCents("60.5"));
        assertEquals(6055, AddRecordMoney.parseCents("60.55"));
        assertEquals(6000, AddRecordMoney.parseCents("60."));
        assertEquals(50, AddRecordMoney.parseCents(".5"));
        assertEquals(5, AddRecordMoney.parseCents("0.05"));
        assertEquals(6000, AddRecordMoney.parseCents("0060"));
        assertEquals(9_999_999_999L, AddRecordMoney.parseCents("99999999.99"));
    }

    @Test
    public void parseCents_invalid() {
        assertEquals(AddRecordMoney.INVALID, AddRecordMoney.parseCents("."));
        assertEquals(AddRecordMoney.INVALID, AddRecordMoney.parseCents("1.234"));
        assertEquals(AddRecordMoney.INVALID, AddRecordMoney.parseCents("-5"));
        assertEquals(AddRecordMoney.INVALID, AddRecordMoney.parseCents("1,000"));
        assertEquals(AddRecordMoney.INVALID, AddRecordMoney.parseCents("abc"));
        assertEquals(AddRecordMoney.INVALID, AddRecordMoney.parseCents("123456789"));
    }

    @Test
    public void tooManyDecimals() {
        assertTrue(AddRecordMoney.hasTooManyDecimals("1.234"));
        assertFalse(AddRecordMoney.hasTooManyDecimals("1.23"));
        assertFalse(AddRecordMoney.hasTooManyDecimals("."));
        assertFalse(AddRecordMoney.hasTooManyDecimals(null));
    }

    @Test
    public void formatYuan() {
        assertEquals("60", AddRecordMoney.formatYuan(6000));
        assertEquals("60.50", AddRecordMoney.formatYuan(6050));
        assertEquals("0.05", AddRecordMoney.formatYuan(5));
        assertEquals("0", AddRecordMoney.formatYuan(0));
        assertEquals("-1.20", AddRecordMoney.formatYuan(-120));
    }

    @Test
    public void fields_followRecordType() {
        AddRecordFields f = AddRecordFields.of(FarmType.FERTILIZE);
        assertEquals(R.string.add_record_material_fertilize, f.materialLabel);
        assertEquals(R.string.add_record_amount, f.amountLabel);
        assertEquals("kg", f.defaultUnit());
        assertEquals("化肥", f.ledgerCategory);
        assertTrue(f.materialRequired());

        assertEquals(R.string.add_record_material_spray, AddRecordFields.of(FarmType.SPRAY).materialLabel);
        assertTrue(AddRecordFields.of(FarmType.SPRAY).units.contains("倍"));
        assertEquals(R.string.add_record_amount_water, AddRecordFields.of(FarmType.IRRIGATE).amountLabel);

        AddRecordFields prune = AddRecordFields.of(FarmType.PRUNE);
        assertEquals(R.string.add_record_material_prune, prune.materialLabel);
        assertFalse(prune.hasAmount());
        assertNull(prune.defaultUnit());

        AddRecordFields harvest = AddRecordFields.of(FarmType.HARVEST);
        assertFalse(harvest.hasMaterial());
        assertTrue(harvest.amountRequired);
        assertEquals(R.string.add_record_amount_harvest, harvest.amountLabel);

        assertEquals(R.string.add_record_material_other, AddRecordFields.of(FarmType.OTHER).materialLabel);
    }

    @Test
    public void dayPart() {
        assertEquals(R.string.add_record_morning, AddRecordFields.dayPartRes(9));
        assertEquals(R.string.add_record_afternoon, AddRecordFields.dayPartRes(12));
        assertEquals(R.string.add_record_afternoon, AddRecordFields.dayPartRes(17));
        assertEquals(R.string.add_record_evening, AddRecordFields.dayPartRes(18));
    }
}
