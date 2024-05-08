package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

/**
 * key-meta-server, crud of key-meta
 * Created by caojiajun on 2024/4/9
 */
public interface KeyMetaServer {

    /**
     * get key-meta by key
     * @param key key
     * @return key-meta
     */
    KeyMeta getKeyMeta(byte[] key);

    /**
     * create or update key-meta
     * @param key key
     * @param keyMeta key-meta
     */
    void createOrUpdateKeyMeta(byte[] key, KeyMeta keyMeta);

    /**
     * delete key-meta
     * @param key key
     */
    void deleteKeyMeta(byte[] key);

    /**
     * exists key-meta
     * @param key key
     * @return true/false
     */
    boolean existsKeyMeta(byte[] key);

    /**
     * check key-meta, if key-meta expired, will delete
     * @param key key
     */
    void checkKeyMetaExpired(byte[] key);
}
