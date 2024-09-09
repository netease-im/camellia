package com.netease.nim.camellia.redis.proxy.upstream.kv.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
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
import com.netease.nim.camellia.redis.proxy.util.MpscSlotHashExecutor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/7
 */
public abstract class Commander {

    public final KVClient kvClient;
    public final KeyDesign keyDesign;
    public final KvConfig kvConfig;
    protected final CacheConfig cacheConfig;
    protected final KeyMetaServer keyMetaServer;
    protected final RedisTemplate cacheRedisTemplate;
    protected final RedisTemplate storageRedisTemplate;
    protected final KvGcExecutor gcExecutor;
    protected final MpscSlotHashExecutor asyncWriteExecutor = KvExecutors.getInstance().getAsyncWriteExecutor();
    protected final WriteBuffer<RedisHash> hashWriteBuffer;
    protected final WriteBuffer<RedisZSet> zsetWriteBuffer;
    protected final WriteBuffer<RedisSet> setWriteBuffer;

    public Commander(CommanderConfig commanderConfig) {
        this.kvClient = commanderConfig.getKvClient();
        this.keyDesign = commanderConfig.getKeyDesign();
        this.cacheConfig = commanderConfig.getCacheConfig();
        this.kvConfig = commanderConfig.getKvConfig();
        this.keyMetaServer = commanderConfig.getKeyMetaServer();
        this.cacheRedisTemplate = commanderConfig.getCacheRedisTemplate();
        this.storageRedisTemplate = commanderConfig.getStorageRedisTemplate();
        this.gcExecutor = commanderConfig.getGcExecutor();
        this.hashWriteBuffer = commanderConfig.getHashWriteBuffer();
        this.zsetWriteBuffer = commanderConfig.getZsetWriteBuffer();
        this.setWriteBuffer = commanderConfig.getSetWriteBuffer();
    }

    /**
     * for read command
     * run to completion thread
     * only contains lru cache read
     */
    public Reply runToCompletion(int slot, Command command) {
        return null;
    }

    public abstract RedisCommand redisCommand();

    protected abstract boolean parse(Command command);

    protected abstract Reply execute(int slot, Command command);

    protected final Reply sync(CompletableFuture<Reply> future) {
        return cacheRedisTemplate.sync(future, cacheConfig.cacheTimeoutMillis());
    }

    protected final List<Reply> sync(List<CompletableFuture<Reply>> futures) {
        return cacheRedisTemplate.sync(futures, cacheConfig.cacheTimeoutMillis());
    }

    protected final void submitAsyncWriteTask(int slot, Result result, Runnable runnable) {
        try {
            asyncWriteExecutor.submit(slot, () -> {
                try {
                    runnable.run();
                } finally {
                    result.kvWriteDone();
                }
            });
        } catch (Exception e) {
            result.kvWriteDone();
            throw e;
        }
    }
}
