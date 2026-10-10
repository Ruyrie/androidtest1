package com.heban.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordStrengthTest {

    @Test
    public void empty_isNone() {
        for (String s : new String[]{null, ""}) {
            PasswordStrength.Result r = PasswordStrength.evaluate(s);
            assertEquals(PasswordStrength.Level.NONE, r.level);
            assertFalse(r.valid());
            assertEquals("未输入", r.label());
            assertEquals(0, r.progress());
        }
    }

    @Test
    public void lengthBoundaries() {
        assertFalse(PasswordStrength.evaluate("abc1234").lengthOk);           // 7 位
        assertTrue(PasswordStrength.evaluate("abc12345").lengthOk);           // 8 位
        assertTrue(PasswordStrength.evaluate("abcdefghij1234567890").lengthOk);   // 20 位
        assertFalse(PasswordStrength.evaluate("abcdefghij1234567890x").lengthOk); // 21 位
    }

    @Test
    public void missingRule_isWeakAndInvalid() {
        PasswordStrength.Result onlyLetters = PasswordStrength.evaluate("abcdefgh");
        assertTrue(onlyLetters.hasLetter);
        assertFalse(onlyLetters.hasDigit);
        assertFalse(onlyLetters.valid());
        assertEquals(PasswordStrength.Level.WEAK, onlyLetters.level);
        assertEquals("弱", onlyLetters.label());
        assertEquals(33, onlyLetters.progress());

        PasswordStrength.Result onlyDigits = PasswordStrength.evaluate("12345678");
        assertFalse(onlyDigits.hasLetter);
        assertFalse(onlyDigits.valid());
        assertEquals(PasswordStrength.Level.WEAK, onlyDigits.level);

        PasswordStrength.Result tooShort = PasswordStrength.evaluate("ab12");
        assertFalse(tooShort.valid());
        assertEquals(PasswordStrength.Level.WEAK, tooShort.level);
    }

    @Test
    public void valid_lowercaseAndDigitsUnder12_isMedium() {
        PasswordStrength.Result r = PasswordStrength.evaluate("abc12345");
        assertTrue(r.valid());
        assertEquals(PasswordStrength.Level.MEDIUM, r.level);
        assertEquals("中", r.label());
        assertEquals(66, r.progress());
        assertEquals(PasswordStrength.Level.MEDIUM, PasswordStrength.evaluate("abcd1234567").level); // 11 位
    }

    @Test
    public void strong_whenLong_mixedCase_orSymbol() {
        assertEquals(PasswordStrength.Level.STRONG, PasswordStrength.evaluate("abcd12345678").level); // 12 位
        assertEquals(PasswordStrength.Level.STRONG, PasswordStrength.evaluate("Abc12345").level);
        assertEquals(PasswordStrength.Level.STRONG, PasswordStrength.evaluate("abc1234!").level);
        PasswordStrength.Result r = PasswordStrength.evaluate("Abc12345");
        assertEquals("强", r.label());
        assertEquals(100, r.progress());
    }
}
