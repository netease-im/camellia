package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.resource.RedisResource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class AsyncCamelliaRedisClient implements AsyncClient {

    private final RedisResource redisResource;
    private final RedisClientAddr addr;

    public AsyncCamelliaRedisClient(RedisResource redisResource) {
        this.redisResource = redisResource;
        this.addr = new RedisClientAddr(redisResource.getHost(), redisResource.getPort(), redisResource.getPassword());
    }

    public void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        RedisClient client = RedisClientHub.get(addr);
        if (client != null) {
            client.sendCommand(commands, completableFutureList);
        } else {
            String log = "RedisClient[" + addr + "] is null, command return NOT_AVAILABLE, RedisResource = " + redisResource.getUrl();
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(AsyncCamelliaRedisClient.class, log);
            }
        }
    }
}
