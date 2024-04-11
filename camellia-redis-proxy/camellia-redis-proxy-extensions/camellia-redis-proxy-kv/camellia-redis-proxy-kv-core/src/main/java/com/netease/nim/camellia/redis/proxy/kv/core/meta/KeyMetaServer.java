package com.netease.nim.camellia.redis.proxy.kv.core.meta;

/**
 * Created by caojiajun on 2024/4/9
 */
public interface KeyMetaServer {

    KeyMeta getKeyMeta(byte[] key, KeyType keyType, boolean createIfNotExists);

    void createOrUpdateKeyMeta(byte[] key, KeyMeta keyMeta);

    int deleteKeyMeta(byte[] key);

    int existsKeyMeta(byte[] key);
}
