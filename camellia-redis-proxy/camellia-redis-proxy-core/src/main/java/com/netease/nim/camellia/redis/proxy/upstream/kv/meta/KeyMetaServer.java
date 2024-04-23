package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

/**
 * Created by caojiajun on 2024/4/9
 */
public interface KeyMetaServer {

    KeyMeta getKeyMeta(byte[] key);

    void createOrUpdateKeyMeta(byte[] key, KeyMeta keyMeta);

    void deleteKeyMeta(byte[] key);

    boolean existsKeyMeta(byte[] key);
}
