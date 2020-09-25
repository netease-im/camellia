package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.ArrayList;
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
        for (Command command : commands) {
            if (command.getRedisCommand().isBlocking()) {
                hasBlockingCommands = true;
                break;
            }
        }
        if (!hasBlockingCommands) {
            flushNoBlockingCommands(commands, completableFutureList);
            return;
        }

        if (commands.size() == 1) {
            flushBlockingCommands(commands, completableFutureList);
            return;
        }

        List<Command> commands1 = new ArrayList<>(commands.size());
        List<CompletableFuture<Reply>> completableFutureList1 = new ArrayList<>(commands.size());
        for (int i=0; i<commands.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = completableFutureList.get(i);
            commands1.add(command);
            completableFutureList1.add(future);
            if (command.getRedisCommand().isBlocking()) {
                flushBlockingCommands(commands1, completableFutureList1);
                commands1 = new ArrayList<>(commands.size());
                completableFutureList1 = new ArrayList<>(commands.size());
            }
        }
        if (!commands1.isEmpty()) {
            flushNoBlockingCommands(commands1, completableFutureList1);
        }
    }

    private void flushBlockingCommands(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        RedisClient client = RedisClientHub.newClient(getAddr());
        if (client != null) {
            client.sendCommand(commands, completableFutureList);
            RedisClientHub.delayStopIfIdle(client);
        } else {
            String log = "RedisClient[" + getAddr() + "] is null, command return NOT_AVAILABLE, RedisResource = " + getResource().getUrl();
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(AsyncCamelliaRedisClient.class, log);
            }
        }
    }

    private void flushNoBlockingCommands(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
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
