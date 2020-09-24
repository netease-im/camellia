package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2020/9/23
 */
public abstract class AsyncCamelliaSimpleClient implements AsyncClient {

    public abstract RedisClientAddr getAddr();
    public abstract Resource getResource();

    public void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        boolean hasBlockingCommands = false;
        List<Command> filterCommands = null;
        List<CompletableFuture<Reply>> filterFutures = null;
        for (int i=0; i<commands.size(); i++) {
            Command command = commands.get(i);
            boolean blocking = command.getRedisCommand().isBlocking();
            if (blocking) {
                RedisClient client = RedisClientHub.newClient(getAddr());
                if (client != null) {
                    CompletableFuture<Reply> future = completableFutureList.get(i);
                    client.sendCommand(Collections.singletonList(command), Collections.singletonList(future));
                    RedisClientHub.delayStopIfIdle(client);
                } else {
                    String log = "RedisClient[" + getAddr() + "] is null, command return NOT_AVAILABLE, RedisResource = " + getResource().getUrl();
                    for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                        completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                        ErrorLogCollector.collect(AsyncCamelliaRedisClient.class, log);
                    }
                }
                hasBlockingCommands = true;
            }
            if (hasBlockingCommands) {
                if (!blocking) {
                    if (filterCommands == null) {
                        filterCommands = new ArrayList<>();
                    }
                    if (filterFutures == null) {
                        filterFutures = new ArrayList<>();
                    }
                    filterCommands.add(command);
                    filterFutures.add(completableFutureList.get(i));
                }
            }
        }

        if (hasBlockingCommands) {
            if (filterCommands == null) return;
            commands = filterCommands;
            completableFutureList = filterFutures;
        }
        RedisClient client = RedisClientHub.get(getAddr());
        if (client != null) {
            client.sendCommand(commands, completableFutureList);
        } else {
            String log = "RedisClient[" + getAddr() + "] is null, command return NOT_AVAILABLE, RedisResource = " + getResource().getUrl();
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(AsyncCamelliaRedisClient.class, log);
            }
        }
    }
}
