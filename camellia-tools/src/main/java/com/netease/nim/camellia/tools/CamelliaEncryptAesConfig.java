package com.netease.nim.camellia.tools;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Created by caojiajun on 2021/8/16
 */
public class CamelliaEncryptAesConfig {
    private static final byte[] default_iv = "0000000000000000".getBytes(StandardCharsets.UTF_8);
    private static final int key_len = 128;

    private final Type type;
    private final IvParameterSpec ivParameterSpec;
    private final SecretKeySpec secretKeySpec;

    public static enum Type {
        CBC_PKCS5PADDING(CamelliaEncryptor.Tag.AES_CBC_PKCS5PADDING, "AES/CBC/PKCS5Padding"),
        ;
        private final String desc;
        private final CamelliaEncryptor.Tag tag;

        Type(CamelliaEncryptor.Tag tag, String desc) {
            this.tag = tag;
            this.desc = desc;
        }
        public CamelliaEncryptor.Tag getTag() {
            return tag;
        }
        public String getDesc() {
            return desc;
        }
    }

    /**
     * 生成AesConfig
     * @param seed 生成秘钥的种子
     */
    public CamelliaEncryptAesConfig(String seed) {
        this(Type.CBC_PKCS5PADDING, seed.getBytes(StandardCharsets.UTF_8));
    }

    public CamelliaEncryptAesConfig(byte[] seed) {
        this(Type.CBC_PKCS5PADDING, seed);
    }

    public CamelliaEncryptAesConfig(Type type, byte[] seed) {
        this(type, seed, default_iv);
    }

    public CamelliaEncryptAesConfig(Type type, byte[] seed, byte[] iv) {
        this(type, key_len, seed, iv);
    }

    public CamelliaEncryptAesConfig(Type type, int keyLen, byte[] seed, byte[] iv) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(seed);
            kgen.init(keyLen, sr);
            SecretKey sKey = kgen.generateKey();
            this.type = type;
            this.secretKeySpec = new SecretKeySpec(sKey.getEncoded(), "AES");
            this.ivParameterSpec = new IvParameterSpec(iv);
        } catch (Exception e) {
            throw new CamelliaEncryptException(e);
        }
    }

    public CamelliaEncryptAesConfig(Type type, IvParameterSpec ivParameterSpec, SecretKeySpec secretKeySpec) {
        this.type = type;
        this.ivParameterSpec = ivParameterSpec;
        this.secretKeySpec = secretKeySpec;
    }

    public Type getType() {
        return type;
    }

    public IvParameterSpec getIvParameterSpec() {
        return ivParameterSpec;
    }

    public SecretKeySpec getSecretKeySpec() {
        return secretKeySpec;
    }
}
