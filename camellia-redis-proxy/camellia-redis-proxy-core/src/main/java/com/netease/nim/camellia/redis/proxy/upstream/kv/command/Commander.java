package com.netease.nim.camellia.redis.proxy.upstream.kv.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMetaServer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/7
 */
public abstract class Commander {

    protected final KVClient kvClient;
    protected final KeyStruct keyStruct;
    protected final CacheConfig cacheConfig;
    protected final KvConfig kvConfig;
    protected final KeyMetaServer keyMetaServer;
    protected final RedisTemplate cacheRedisTemplate;
    protected final KvGcExecutor gcExecutor;

    public Commander(CommanderConfig commanderConfig) {
        this.kvClient = commanderConfig.getKvClient();
        this.keyStruct = commanderConfig.getKeyStruct();
        this.cacheConfig = commanderConfig.getCacheConfig();
        this.kvConfig = commanderConfig.getKvConfig();
        this.keyMetaServer = commanderConfig.getKeyMetaServer();
        this.cacheRedisTemplate = commanderConfig.getRedisTemplate();
        this.gcExecutor = commanderConfig.getGcExecutor();
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
}
