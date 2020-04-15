package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
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

    private RedisResource redisResource;

    public AsyncCamelliaRedisClient(RedisResource redisResource) {
        this.redisResource = redisResource;
        RedisClient client = RedisClientHub.get(redisResource.getHost(), redisResource.getPort(), redisResource.getPassword());
        if (client == null) {
            throw new CamelliaRedisException("RedisClient init fail");
        }
    }

    public void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        CompletableFuture<RedisClient> future = RedisClientHub.getAsync(redisResource.getHost(), redisResource.getPort(), redisResource.getPassword());
        future.thenAccept(client -> {
            if (client != null) {
                client.sendCommand(commands, completableFutureList);
            } else {
                String log = "RedisClient[" + redisResource.getUrl() + "] is null, command return NOT_AVAILABLE";
                for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                    completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                    ErrorLogCollector.collect(AsyncCamelliaRedisClient.class, log);
                }
            }
        });
    }
}
