package com.netease.nim.camellia.redis.proxy.kv.core.meta;


import com.netease.nim.camellia.redis.proxy.kv.core.utils.BytesUtils;

/**
 * Created by caojiajun on 2024/4/7
 */
public class KeyMeta {
    private final KeyType keyType;
    private final long version;
    private final long expireTime;
    private byte[] _data;

    private KeyMeta(KeyType keyType, long version, long expireTime, byte[] _data) {
        this.keyType = keyType;
        this.version = version;
        this.expireTime = expireTime;
        this._data = _data;
    }

    public KeyMeta(KeyType keyType, long version, long expireTime) {
        this.keyType = keyType;
        this.version = version;
        this.expireTime = expireTime;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public long getVersion() {
        return version;
    }

    public byte[] toBytes() {
        if (_data != null) {
            return _data;
        }
        _data = BytesUtils.merge(keyType.getValue(), BytesUtils.toBytes(expireTime));
        return _data;
    }

    public static KeyMeta fromBytes(byte[] data) {
        if (data == null) {
            return null;
        }
        if (data.length != 17) {
            return null;
        }
        KeyType keyType = KeyType.getByValue(data[0]);
        long version = BytesUtils.toLong(data, 1);
        long expireTime = BytesUtils.toLong(data, 9);
        return new KeyMeta(keyType, version, expireTime, data);
    }
}
