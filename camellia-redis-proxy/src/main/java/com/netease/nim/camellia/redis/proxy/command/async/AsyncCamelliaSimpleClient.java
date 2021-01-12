package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
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
        List<Command> filterCommands = new ArrayList<>(commands.size());
        List<CompletableFuture<Reply>> filterFutures = new ArrayList<>(completableFutureList.size());
        boolean hasBlockingCommands = false;

        for (int i=0; i<commands.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = completableFutureList.get(i);
            ChannelInfo channelInfo = command.getChannelInfo();
            RedisClient bindClient = channelInfo.getBindClient();
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
                if (bindClient == null) {
                    bindClient = RedisClientHub.newClient(getAddr());
                    channelInfo.setBindClient(bindClient);
                }
                if (bindClient != null) {
                    AsyncTaskQueue asyncTaskQueue = command.getChannelInfo().getAsyncTaskQueue();
                    PubSubUtils.sendByBindClient(bindClient, asyncTaskQueue, command, future);
                } else {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                }
            } else if (redisCommand.getCommandType() == RedisCommand.CommandType.TRANSACTION) {
                if (bindClient == null) {
                    bindClient = RedisClientHub.newClient(getAddr());
                    channelInfo.setBindClient(bindClient);
                }
                if (bindClient == null) {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                } else {
                    bindClient.sendCommand(Collections.singletonList(command), Collections.singletonList(future));
                    if (redisCommand == RedisCommand.MULTI) {
                        channelInfo.setInTransaction(true);
                    } else if (redisCommand == RedisCommand.EXEC || redisCommand == RedisCommand.DISCARD) {
                        channelInfo.setInTransaction(false);
                        channelInfo.setBindClient(null);
                        RedisClientHub.delayStopIfIdle(bindClient);
                    } else if (redisCommand == RedisCommand.UNWATCH) {
                        if (!channelInfo.isInTransaction()) {
                            channelInfo.setBindClient(null);
                            RedisClientHub.delayStopIfIdle(bindClient);
                        }
                    }
                }
            } else {
                if (bindClient != null) {
                    bindClient.sendCommand(Collections.singletonList(command), Collections.singletonList(future));
                } else {
                    filterCommands.add(command);
                    filterFutures.add(future);
                    if (command.isBlocking()) {
                        hasBlockingCommands = true;
                    }
                }
            }
        }
        if (filterCommands.isEmpty()) return;
        commands = filterCommands;
        completableFutureList = filterFutures;

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
            if (command.isBlocking()) {
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
            Command lastBlockingCommand = commands.get(commands.size() - 1);
            lastBlockingCommand.getChannelInfo().addRedisClientForBlockingCommand(client);
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
