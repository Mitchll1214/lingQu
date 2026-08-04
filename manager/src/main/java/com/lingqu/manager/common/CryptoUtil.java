package com.lingqu.manager.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES 加密工具，用于数据源密码、Token 的加密存储。
 * 算法：AES/ECB/PKCS5Padding，密钥 16 字节（可配置 AES_KEY 环境变量，默认 lingqu-aes-key-01）。
 */
public final class CryptoUtil {

    private static volatile SecretKeySpec keySpec;

    private CryptoUtil() {
    }

    /**
     * 使用固定密钥字符串初始化。密钥会被截断/补齐到 16 字节（AES-128）。
     */
    public static synchronized void init(String key) {
        if (key == null || key.isEmpty()) {
            key = "lingqu-aes-key-01";
        }
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        byte[] k = new byte[16];
        System.arraycopy(raw, 0, k, 0, Math.min(raw.length, 16));
        keySpec = new SecretKeySpec(k, "AES");
    }

    private static SecretKeySpec key() {
        if (keySpec == null) {
            init(null);
        }
        return keySpec;
    }

    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key());
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES decrypt failed", e);
        }
    }
}
