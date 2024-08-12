package com.netease.nim.camellia.redis.proxy.upstream.kv.command;


import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBuffer;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyDesign;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMetaServer;

/**
 * Created by caojiajun on 2024/4/8
 */
public class CommanderConfig {

    private final KVClient kvClient;
    private final KeyDesign keyDesign;
    private final CacheConfig cacheConfig;
    private final KvConfig kvConfig;
    private final KeyMetaServer keyMetaServer;
    private final RedisTemplate cacheRedisTemplate;
    private final RedisTemplate storageRedisTemplate;
    private final KvGcExecutor gcExecutor;
    private final WriteBuffer<RedisHash> hashWriteBuffer;
    private final WriteBuffer<RedisZSet> zsetWriteBuffer;
    private final WriteBuffer<RedisSet> setWriteBuffer;

    public CommanderConfig(KVClient kvClient, KeyDesign keyDesign, CacheConfig cacheConfig,
                           KvConfig kvConfig, KeyMetaServer keyMetaServer,
                           RedisTemplate cacheRedisTemplate, RedisTemplate storageRedisTemplate, KvGcExecutor gcExecutor,
                           WriteBuffer<RedisHash> hashWriteBuffer, WriteBuffer<RedisZSet> zsetWriteBuffer, WriteBuffer<RedisSet> setWriteBuffer) {
        this.kvClient = kvClient;
        this.keyDesign = keyDesign;
        this.cacheConfig = cacheConfig;
        this.kvConfig = kvConfig;
        this.keyMetaServer = keyMetaServer;
        this.cacheRedisTemplate = cacheRedisTemplate;
        this.storageRedisTemplate = storageRedisTemplate;
        this.gcExecutor = gcExecutor;
        this.hashWriteBuffer = hashWriteBuffer;
        this.zsetWriteBuffer = zsetWriteBuffer;
        this.setWriteBuffer = setWriteBuffer;
    }

    public KVClient getKvClient() {
        return kvClient;
    }

    public KeyDesign getKeyDesign() {
        return keyDesign;
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

    public RedisTemplate getCacheRedisTemplate() {
        return cacheRedisTemplate;
    }

    public RedisTemplate getStorageRedisTemplate() {
        return storageRedisTemplate;
    }

    public KvGcExecutor getGcExecutor() {
        return gcExecutor;
    }

    public WriteBuffer<RedisHash> getHashWriteBuffer() {
        return hashWriteBuffer;
    }

    public WriteBuffer<RedisZSet> getZsetWriteBuffer() {
        return zsetWriteBuffer;
    }

    public WriteBuffer<RedisSet> getSetWriteBuffer() {
        return setWriteBuffer;
    }
}

