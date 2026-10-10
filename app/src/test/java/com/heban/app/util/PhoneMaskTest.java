package com.heban.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PhoneMaskTest {

    @Test
    public void isValid_mainlandMobileNumbers() {
        assertTrue(PhoneMask.isValid("13800005678"));
        assertTrue(PhoneMask.isValid("19912345678"));
        assertFalse(PhoneMask.isValid(null));
        assertFalse(PhoneMask.isValid(""));
        assertFalse(PhoneMask.isValid("1380000567"));     // 10 位
        assertFalse(PhoneMask.isValid("138000056789"));   // 12 位
        assertFalse(PhoneMask.isValid("12800005678"));    // 第二位不是 3–9
        assertFalse(PhoneMask.isValid("23800005678"));
        assertFalse(PhoneMask.isValid("1380000567a"));
        assertFalse(PhoneMask.isValid(" 13800005678"));
    }

    @Test
    public void mask_hidesMiddleFourDigits() {
        assertEquals("138****5678", PhoneMask.mask("13800005678"));
    }

    @Test
    public void mask_invalidInputUnchanged() {
        assertEquals("", PhoneMask.mask(null));
        assertEquals("", PhoneMask.mask(""));
        assertEquals("12345", PhoneMask.mask("12345"));
        assertEquals("138000056789", PhoneMask.mask("138000056789"));
    }
}
