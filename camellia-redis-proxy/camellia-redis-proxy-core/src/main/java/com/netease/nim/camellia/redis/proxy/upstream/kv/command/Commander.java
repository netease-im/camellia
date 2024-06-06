package com.netease.nim.camellia.redis.proxy.upstream.kv.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBuffer;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.Hash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyDesign;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMetaServer;
import com.netease.nim.camellia.redis.proxy.upstream.utils.MpscHashedExecutor;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/7
 */
public abstract class Commander {

    protected final KVClient kvClient;
    protected final KeyDesign keyDesign;
    protected final CacheConfig cacheConfig;
    protected final KvConfig kvConfig;
    protected final KeyMetaServer keyMetaServer;
    protected final RedisTemplate cacheRedisTemplate;
    protected final RedisTemplate storeRedisTemplate;
    protected final KvGcExecutor gcExecutor;
    protected final MpscHashedExecutor asyncWriteExecutor = KvExecutors.getInstance().getAsyncWriteExecutor();
    protected final WriteBuffer<Hash> hashWriteBuffer;
    protected final WriteBuffer<ZSet> zsetWriteBuffer;

    public Commander(CommanderConfig commanderConfig) {
        this.kvClient = commanderConfig.getKvClient();
        this.keyDesign = commanderConfig.getKeyDesign();
        this.cacheConfig = commanderConfig.getCacheConfig();
        this.kvConfig = commanderConfig.getKvConfig();
        this.keyMetaServer = commanderConfig.getKeyMetaServer();
        this.cacheRedisTemplate = commanderConfig.getCacheRedisTemplate();
        this.storeRedisTemplate = commanderConfig.getStoreRedisTemplate();
        this.gcExecutor = commanderConfig.getGcExecutor();
        this.hashWriteBuffer = commanderConfig.getHashWriteBuffer();
        this.zsetWriteBuffer = commanderConfig.getZsetWriteBuffer();
    }

    public abstract RedisCommand redisCommand();

    protected abstract boolean parse(Command command);

    protected abstract Reply execute(Command command);

    protected final Reply sync(CompletableFuture<Reply> future) {
        return cacheRedisTemplate.sync(future, cacheConfig.cacheTimeoutMillis());
    }

    protected final List<Reply> sync(List<CompletableFuture<Reply>> futures) {
        return cacheRedisTemplate.sync(futures, cacheConfig.cacheTimeoutMillis());
    }

    protected final Reply checkCache(byte[] script, byte[] cacheKey, byte[][] args) {
        //cache
        Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, args));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            String type = Utils.bytesToString(((BulkReply) replies[0]).getRaw());
            if (type.equalsIgnoreCase("1")) {//cache hit
                return replies[1];
            }
        }
        return null;
    }

    protected final void submitAsyncWriteTask(byte[] cacheKey, Result result, Runnable runnable) {
        try {
            asyncWriteExecutor.submit(cacheKey, () -> {
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
