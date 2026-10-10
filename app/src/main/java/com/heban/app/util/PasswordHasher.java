package com.heban.app.util;

import androidx.annotation.Nullable;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 模拟账号的密码哈希（技术准备文档 7.4）：PBKDF2WithHmacSHA256，随机 16 字节盐，10000 次迭代，256 位结果。
 * 盐和哈希都以 Base64 字符串保存到 user 表的 salt / password_hash 列。
 */
public final class PasswordHasher {
    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int ITERATIONS = 10000;
    public static final int KEY_BITS = 256;
    public static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** 新的随机盐（Base64） */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** 计算密码哈希（Base64）；salt 为 {@link #newSalt()} 生成的 Base64 字符串 */
    public static String hash(String password, String salt) {
        return Base64.getEncoder().encodeToString(derive(password, Base64.getDecoder().decode(salt)));
    }

    /** 比对密码；任一参数为空或格式错误时返回 false。哈希比较用时恒定。 */
    public static boolean verify(@Nullable String password, @Nullable String salt,
                                 @Nullable String expectedHash) {
        if (password == null || password.isEmpty() || salt == null || expectedHash == null) {
            return false;
        }
        byte[] expected;
        byte[] saltBytes;
        try {
            expected = Base64.getDecoder().decode(expectedHash);
            saltBytes = Base64.getDecoder().decode(salt);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(derive(password, saltBytes), expected);
    }

    private static byte[] derive(String password, byte[] salt) {
        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHM + " 不可用", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(chars, '\0');
        }
    }
}
