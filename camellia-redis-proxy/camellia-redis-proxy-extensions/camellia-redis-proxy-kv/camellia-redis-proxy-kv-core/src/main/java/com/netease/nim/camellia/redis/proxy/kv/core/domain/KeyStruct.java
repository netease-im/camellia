package com.netease.nim.camellia.redis.proxy.kv.core.domain;

import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.kv.core.utils.BytesUtils;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2024/4/7
 */
public class KeyStruct {

    private static final byte[] HASH_TAG_LEFT = "{".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HASH_TAG_RIGHT = "}".getBytes(StandardCharsets.UTF_8);

    private final byte[] namespace;
    private final byte[] metaPrefix;
    private final byte[] cachePrefix;

    public KeyStruct(byte[] namespace) {
        this.namespace = namespace;
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
        long version = keyMeta.getVersion();
        return BytesUtils.merge(cachePrefix, key, BytesUtils.toBytes(version));
    }

    public byte[] hashFieldCacheKey(KeyMeta keyMeta, byte[] key, byte[] field) {
        long version = keyMeta.getVersion();
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
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getVersion()));
        if (field.length > 0) {
            data = BytesUtils.merge(data, field);
        }
        return data;
    }
}
