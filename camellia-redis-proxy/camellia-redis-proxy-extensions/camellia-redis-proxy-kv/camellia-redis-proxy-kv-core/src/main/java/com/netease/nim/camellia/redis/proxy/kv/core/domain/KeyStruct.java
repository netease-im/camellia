package com.netease.nim.camellia.redis.proxy.kv.core.domain;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.kv.core.exception.KvException;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMetaVersion;
import com.netease.nim.camellia.redis.proxy.kv.core.utils.BytesUtils;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2024/4/7
 */
public class KeyStruct {

    private static final byte[] HASH_TAG_LEFT = "{".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HASH_TAG_RIGHT = "}".getBytes(StandardCharsets.UTF_8);

    private final int namespaceLen;
    private final byte[] namespace;
    private final byte[] metaPrefix;
    private final byte[] cachePrefix;

    public KeyStruct(byte[] namespace) {
        this.namespace = namespace;
        this.namespaceLen = namespace.length;
        this.metaPrefix = BytesUtils.merge("m#".getBytes(StandardCharsets.UTF_8), namespace);
        this.cachePrefix = BytesUtils.merge("c#".getBytes(StandardCharsets.UTF_8), namespace);
    }

    public byte[] getNamespace() {
        return namespace;
    }

    public byte[] metaKey(byte[] key) {
        return BytesUtils.merge(metaPrefix, key);
    }

    public byte[] cacheKey(KeyMeta keyMeta, byte[] key) {
        long version = keyMeta.getKeyVersion();
        return BytesUtils.merge(cachePrefix, key, BytesUtils.toBytes(version));
    }

    public byte[] hashFieldCacheKey(KeyMeta keyMeta, byte[] key, byte[] field) {
        long version = keyMeta.getKeyVersion();
        byte[] data = BytesUtils.merge(cachePrefix, key, BytesUtils.toBytes(version));
        data = BytesUtils.merge(HASH_TAG_LEFT, data, HASH_TAG_RIGHT);
        if (field.length > 0) {
            data = BytesUtils.merge(data, field);
        }
        return data;
    }

    public byte[] hashFieldStoreKey(KeyMeta keyMeta, byte[] key, byte[] field) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(namespace, keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        if (field.length > 0) {
            data = BytesUtils.merge(data, field);
        }
        return data;
    }

    public byte[] decodeHashFieldByStoreKey(byte[] storeKey, byte[] key) {
        int prefixSize = namespaceLen + 4 + key.length + 8;
        int fieldSize = storeKey.length - prefixSize;
        byte[] field = new byte[fieldSize];
        System.arraycopy(storeKey, prefixSize, field, 0, fieldSize);
        return field;
    }

    public KeyMetaVersion hashKeyMetaVersion() {
        int version = ProxyDynamicConf.getInt("kv.hash.key.meta.version", 0);
        if (version == 0) {
            return KeyMetaVersion.version_0;
        } else if (version == 1) {
            return KeyMetaVersion.version_1;
        } else {
            throw new KvException("ERR illegal key meta version");
        }
    }
}
