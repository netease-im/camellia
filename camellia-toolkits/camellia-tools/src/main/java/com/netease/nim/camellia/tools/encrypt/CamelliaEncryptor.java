package com.netease.nim.camellia.tools.encrypt;

import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * 一个加解密的工具类，会在加密数据头里添加相关校验，用于判断数据是否加过密，可以用于对加密前后的数据进行兼容性处理（加过密的则直接解密后返回，没有加过密的则直接返回）
 * 数据结构：tag（1字节，用于判断加密类型）+ 魔数（自定义，n个字节） + 加密后长度（4字节） + 解密后长度（4字节） + 密文（m个字节）
 * Created by caojiajun on 2021/8/13
 */
public class CamelliaEncryptor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaEncryptor.class);

    private static final String DEFAULT_MAGIC = "camellia~e";

    private final CipherInitializer cipherInitializer;
    private final FastThreadLocal<Cipher> encryptThreadLocal = new FastThreadLocal<>();
    private final FastThreadLocal<Cipher> decryptThreadLocal = new FastThreadLocal<>();
    private final Tag tag;
    private final byte[] magicBytes;
    private final int headerLen;

    public static enum Tag {
        AES_CBC_PKCS5PADDING((byte) 1),
        AES_ECB_PKCS5PADDING((byte) 2),
        AES_CTR_NOPADDING((byte) 3),
        AES_CFB_NOPADDING((byte) 4),
        AES_OFB_NOPADDING((byte) 5),
        ;
        private final byte tag;

        Tag(byte tag) {
            this.tag = tag;
        }
        public byte getValue() {
            return tag;
        }
    }

    public CamelliaEncryptor(CamelliaEncryptAesConfig camelliaEncryptAesConfig) {
        this(DEFAULT_MAGIC, camelliaEncryptAesConfig);
    }

    public CamelliaEncryptor(String magic, CamelliaEncryptAesConfig camelliaEncryptAesConfig) {
        this.tag = camelliaEncryptAesConfig.getType().getTag();
        this.cipherInitializer = new CipherInitializer() {
            @Override
            public Cipher initEncrypt() {
                try {
                    Cipher cipher = Cipher.getInstance(camelliaEncryptAesConfig.getType().getDesc());
                    IvParameterSpec ivParameterSpec = camelliaEncryptAesConfig.getIvParameterSpec();
                    if (ivParameterSpec != null) {
                        cipher.init(Cipher.ENCRYPT_MODE, camelliaEncryptAesConfig.getSecretKeySpec(), ivParameterSpec);
                    } else {
                        cipher.init(Cipher.ENCRYPT_MODE, camelliaEncryptAesConfig.getSecretKeySpec());
                    }
                    return cipher;
                } catch (Exception e) {
                    logger.error("initEncrypt error", e);
                    throw new CamelliaEncryptException(e);
                }
            }

            @Override
            public Cipher initDecrypt() {
                try {
                    Cipher cipher = Cipher.getInstance(camelliaEncryptAesConfig.getType().getDesc());
                    IvParameterSpec ivParameterSpec = camelliaEncryptAesConfig.getIvParameterSpec();
                    if (ivParameterSpec != null) {
                        cipher.init(Cipher.DECRYPT_MODE, camelliaEncryptAesConfig.getSecretKeySpec(), ivParameterSpec);
                    } else {
                        cipher.init(Cipher.DECRYPT_MODE, camelliaEncryptAesConfig.getSecretKeySpec());
                    }
                    return cipher;
                } catch (Exception e) {
                    logger.error("initDecrypt error", e);
                    throw new CamelliaEncryptException(e);
                }
            }
        };
        this.magicBytes = magic.getBytes(StandardCharsets.UTF_8);
        this.headerLen = 1 + magicBytes.length + 4 + 4;
    }

    private interface CipherInitializer {
        Cipher initEncrypt();
        Cipher initDecrypt();
    }

    private Cipher getEncryptCipher() {
        Cipher cipher = encryptThreadLocal.get();
        if (cipher == null) {
            cipher = cipherInitializer.initEncrypt();
            encryptThreadLocal.set(cipher);
        }
        return cipher;
    }

    private Cipher getDecryptCipher() {
        Cipher cipher = decryptThreadLocal.get();
        if (cipher == null) {
            cipher = cipherInitializer.initDecrypt();
            decryptThreadLocal.set(cipher);
        }
        return cipher;
    }

    /**
     * 加密
     * @param originalData 原始数据
     * @return 加密后数据
     */
    public byte[] encrypt(byte[] originalData) {
        try {
            if (originalData == null) return null;
            int originalDataLen = originalData.length;
            byte[] encryptText = getEncryptCipher().doFinal(originalData);
            byte[] encryptData = new byte[headerLen + encryptText.length];
            System.arraycopy(encryptText, 0, encryptData, headerLen, encryptText.length);
            ByteBuffer buffer = ByteBuffer.wrap(encryptData);
            buffer.put(tag.getValue());
            buffer.put(magicBytes);
            buffer.putInt(encryptData.length);
            buffer.putInt(originalDataLen);
            return encryptData;
        } catch (Exception e) {
            logger.error("encrypt error", e);
            throw new CamelliaEncryptException(e);
        }
    }

    public String encryptToBase64(byte[] originalData) {
        byte[] encrypt = encrypt(originalData);
        if (encrypt == null) return null;
        return Base64.getEncoder().encodeToString(encrypt);
    }

    /**
     * 解密，会判断数据是否加密
     * @param encryptData 加密后数据
     * @return 原始数据
     */
    public byte[] decrypt(byte[] encryptData) {
        try {
            if (encryptData == null) return null;
            if (encryptData.length <= headerLen) {
                return encryptData;
            }
            ByteBuffer buffer = ByteBuffer.wrap(encryptData);
            byte tag = buffer.get();
            if (tag != this.tag.getValue()) {
                return encryptData;
            }
            byte[] magicTest = new byte[magicBytes.length];
            buffer.get(magicTest);
            if (!Arrays.equals(magicTest, magicBytes)) {
                return encryptData;
            }
            int encryptLen = buffer.getInt();
            int originalLen = buffer.getInt();
            if (encryptData.length != encryptLen) {
                return encryptData;
            }
            byte[] originalData = getDecryptCipher().doFinal(encryptData, headerLen, encryptData.length - headerLen);
            if (originalData.length != originalLen) {
                return encryptData;
            }
            return originalData;
        } catch (Exception e) {
            logger.error("decrypt error", e);
            throw new CamelliaEncryptException(e);
        }
    }

    public byte[] decryptFromBase64(String encryptDataBase64) {
        if (encryptDataBase64 == null) return null;
        return decrypt(Base64.getDecoder().decode(encryptDataBase64));
    }
}
