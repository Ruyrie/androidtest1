package com.heban.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PasswordHasherTest {

    @Test
    public void newSalt_is16RandomBytes() {
        String a = PasswordHasher.newSalt();
        String b = PasswordHasher.newSalt();
        assertEquals(16, Base64.getDecoder().decode(a).length);
        assertNotEquals(a, b);
    }

    @Test
    public void hash_is256BitAndDeterministicForSameSalt() {
        String salt = PasswordHasher.newSalt();
        String h1 = PasswordHasher.hash("abc12345", salt);
        assertEquals(32, Base64.getDecoder().decode(h1).length);
        assertEquals(h1, PasswordHasher.hash("abc12345", salt));
        assertNotEquals(h1, PasswordHasher.hash("abc12345", PasswordHasher.newSalt()));
        assertNotEquals(h1, PasswordHasher.hash("abc12346", salt));
    }

    @Test
    public void verify_acceptsOnlyTheRightPassword() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("Farm2026ok", salt);
        assertTrue(PasswordHasher.verify("Farm2026ok", salt, hash));
        assertFalse(PasswordHasher.verify("farm2026ok", salt, hash));
        assertFalse(PasswordHasher.verify("Farm2026ok", PasswordHasher.newSalt(), hash));
        assertFalse(PasswordHasher.verify("", salt, hash));
        assertFalse(PasswordHasher.verify(null, salt, hash));
        assertFalse(PasswordHasher.verify("Farm2026ok", null, hash));
        assertFalse(PasswordHasher.verify("Farm2026ok", salt, null));
        assertFalse(PasswordHasher.verify("Farm2026ok", "not base64!", hash));
    }

    /** 与 PBKDF2-HMAC-SHA256 定义（RFC 8018，单块 T1 = U1 ^ … ^ U10000）独立计算的结果一致 */
    @Test
    public void hash_matchesPbkdf2Definition() throws Exception {
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) salt[i] = (byte) i;
        String saltB64 = Base64.getEncoder().encodeToString(salt);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("abc12345".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update(salt);
        byte[] u = mac.doFinal(new byte[]{0, 0, 0, 1});
        byte[] t = u.clone();
        for (int i = 1; i < PasswordHasher.ITERATIONS; i++) {
            u = mac.doFinal(u);
            for (int j = 0; j < t.length; j++) t[j] ^= u[j];
        }
        assertEquals(Base64.getEncoder().encodeToString(t), PasswordHasher.hash("abc12345", saltB64));
    }
}
