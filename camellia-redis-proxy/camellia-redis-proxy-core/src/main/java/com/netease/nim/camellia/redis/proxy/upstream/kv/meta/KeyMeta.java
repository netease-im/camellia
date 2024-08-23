package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;


import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.EstimateSizeValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;

/**
 * key-meta
 * Created by caojiajun on 2024/4/7
 */
public class KeyMeta implements EstimateSizeValue {
    private final EncodeVersion encodeVersion;
    private final KeyType keyType;
    private final long keyVersion;
    private final long expireTime;
    private final byte[] extra;

    public KeyMeta(EncodeVersion encodeVersion, KeyType keyType, long keyVersion, long expireTime) {
        this(encodeVersion, keyType, keyVersion, expireTime, null);
    }

    public KeyMeta(EncodeVersion encodeVersion, KeyType keyType, long keyVersion, long expireTime, byte[] extra) {
        this.encodeVersion = encodeVersion;
        this.keyType = keyType;
        this.keyVersion = keyVersion;
        this.expireTime = expireTime;
        this.extra = extra;
    }

    public final KeyType getKeyType() {
        return keyType;
    }

    public final long getExpireTime() {
        return expireTime;
    }

    public final EncodeVersion getEncodeVersion() {
        return encodeVersion;
    }

    public final long getKeyVersion() {
        return keyVersion;
    }

    public final byte[] getExtra() {
        return extra;
    }

    public final boolean isExpire() {
        if (expireTime < 0) {
            return false;
        }
        return expireTime <= System.currentTimeMillis();
    }

    public final byte[] toBytes() {
        byte[] flag = new byte[2];
        flag[0] = encodeVersion.getValue();
        flag[1] = keyType.getValue();
        byte[] data = BytesUtils.merge(flag, BytesUtils.toBytes(keyVersion), BytesUtils.toBytes(expireTime));
        if (extra != null && extra.length > 0) {
            data = BytesUtils.merge(data, extra);
        }
        return data;
    }

    public static KeyMeta fromBytes(byte[] raw) {
        if (raw == null) {
            return null;
        }
        if (raw.length < 18) {
            return null;
        }
        EncodeVersion encodeVersion = EncodeVersion.getByValue(raw[0]);
        KeyType keyType = KeyType.getByValue(raw[1]);
        long keyVersion = BytesUtils.toLong(raw, 2);
        long expireTime = BytesUtils.toLong(raw, 10);
        byte[] extra = null;
        if (raw.length > 18) {
            extra = new byte[raw.length - 18];
            System.arraycopy(raw, 18, extra, 0, extra.length);
        }
        return new KeyMeta(encodeVersion, keyType, keyVersion, expireTime, extra);
    }

    @Override
    public long estimateSize() {
        long estimateSize = 32;
        if (extra != null) {
            estimateSize += extra.length;
        }
        return estimateSize;
    }
}
