package com.netease.nim.camellia.redis.proxy.upstream.kv.command;


import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;
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
    private final KvGcExecutor gcExecutor;

    public CommanderConfig(KVClient kvClient, KeyStruct keyStruct, CacheConfig cacheConfig,
                           KvConfig kvConfig, KeyMetaServer keyMetaServer, RedisTemplate cacheRedisTemplate, KvGcExecutor gcExecutor) {
        this.kvClient = kvClient;
        this.keyStruct = keyStruct;
        this.cacheConfig = cacheConfig;
        this.kvConfig = kvConfig;
        this.keyMetaServer = keyMetaServer;
        this.cacheRedisTemplate = cacheRedisTemplate;
        this.gcExecutor = gcExecutor;
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

    public KvGcExecutor getGcExecutor() {
        return gcExecutor;
    }
}

