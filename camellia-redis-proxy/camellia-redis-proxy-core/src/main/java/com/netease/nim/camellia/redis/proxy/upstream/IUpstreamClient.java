package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2019/12/19.
 */
public interface IUpstreamClient {

    void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> futureList);

    void preheat();

    String getUrl();

    boolean isValid();

    default boolean checkValid(RedisConnectionAddr addr) {
        if (addr == null) return false;
        RedisConnection redisConnection = RedisConnectionHub.getInstance().get(addr);
        return redisConnection != null && redisConnection.isValid();
    }
}
