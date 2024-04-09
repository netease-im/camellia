package com.netease.nim.camellia.redis.proxy.kv.core.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.domain.*;
import com.netease.nim.camellia.redis.proxy.kv.core.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.DefaultKeyMetaServer;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMetaServer;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

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

    public Commander(CommanderConfig commanderConfig) {
        this.kvClient = commanderConfig.getKvClient();
        this.keyStruct = commanderConfig.getKeyStruct();
        this.cacheConfig = commanderConfig.getCacheConfig();
        this.kvConfig = commanderConfig.getKvConfig();
        this.keyMetaServer = commanderConfig.getKeyMetaServer();
        this.cacheRedisTemplate = commanderConfig.getRedisTemplate();
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
