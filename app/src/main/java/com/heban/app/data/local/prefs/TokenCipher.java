package com.heban.app.data.local.prefs;

/** 敏感字符串的加解密（Token、API Key），由 {@link SecureStore} 实现；单元测试中可替换。 */
public interface TokenCipher {
    String encrypt(String plain) throws Exception;

    String decrypt(String encoded) throws Exception;
}
