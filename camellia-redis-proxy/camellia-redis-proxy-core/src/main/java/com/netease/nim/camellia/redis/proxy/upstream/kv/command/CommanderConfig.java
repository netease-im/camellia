package com.netease.nim.camellia.redis.proxy.upstream.kv.command;


import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMetaServer;

/**
 * Created by caojiajun on 2024/4/8
 */
public class CommanderConfig {

    private final KVClient kvClient;
    private final KeyStruct keyStruct;
    private final CacheConfig cacheConfig;
    private final KvConfig kvConfig;
    private final KeyMetaServer keyMetaServer;
    private final RedisTemplate cacheRedisTemplate;

    public CommanderConfig(KVClient kvClient, KeyStruct keyStruct, CacheConfig cacheConfig,
                           KvConfig kvConfig, KeyMetaServer keyMetaServer, RedisTemplate cacheRedisTemplate) {
        this.kvClient = kvClient;
        this.keyStruct = keyStruct;
        this.cacheConfig = cacheConfig;
        this.kvConfig = kvConfig;
        this.keyMetaServer = keyMetaServer;
        this.cacheRedisTemplate = cacheRedisTemplate;
    }

    public KVClient getKvClient() {
        return kvClient;
    }

    public KeyStruct getKeyStruct() {
        return keyStruct;
    }

    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

    public KvConfig getKvConfig() {
        return kvConfig;
    }

    public KeyMetaServer getKeyMetaServer() {
        return keyMetaServer;
    }

    public RedisTemplate getRedisTemplate() {
        return cacheRedisTemplate;
    }
}

