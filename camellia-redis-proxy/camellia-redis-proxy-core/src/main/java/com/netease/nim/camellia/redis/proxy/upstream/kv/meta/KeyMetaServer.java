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
    KeyMeta getKeyMeta(int slot, byte[] key);

    /**
     * create or update key-meta
     * @param key key
     * @param keyMeta key-meta
     */
    void createOrUpdateKeyMeta(int slot, byte[] key, KeyMeta keyMeta);

    /**
     * delete key-meta
     * @param key key
     */
    void deleteKeyMeta(int slot, byte[] key);


    /**
     * check key-meta, if key-meta expired, will delete
     * @param key key
     */
    void checkKeyMetaExpired(int slot, byte[] key);
}
