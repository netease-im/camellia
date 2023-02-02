package com.netease.nim.camellia.redis.proxy.upstream.standalone;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.command.CommandTaskQueue;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.upstream.utils.PubSubUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2020/9/23
 */
public abstract class AbstractSimpleRedisClient implements IUpstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSimpleRedisClient.class);

    public abstract RedisConnectionAddr getAddr();
    public abstract Resource getResource();

    @Override
    public String getUrl() {
        return getResource().getUrl();
    }

    @Override
    public void preheat() {
        if(logger.isInfoEnabled()) {
            logger.info("try preheat, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
        }
        RedisConnectionAddr addr = getAddr();
        boolean result = RedisConnectionHub.getInstance().preheat(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword(), addr.getDb());
        if(logger.isInfoEnabled()) {
            logger.info("preheat result = {}, url = {}", result, PasswordMaskUtils.maskResource(getResource().getUrl()));
        }
    }

    protected boolean checkValid(RedisConnectionAddr addr) {
        if (addr == null) return false;
        RedisConnection redisConnection = RedisConnectionHub.getInstance().get(addr);
        return redisConnection != null && redisConnection.isValid();
    }

    public void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        if (commands.size() == 1) {
            Command command = commands.get(0);
            if (isPassThroughCommand(command)) {
                flushNoBlockingCommands(commands, completableFutureList);
                return;
            }
        } else {
            if (isPassThroughCommands(commands)) {
                flushNoBlockingCommands(commands, completableFutureList);
                return;
            }
        }
        List<Command> filterCommands = new ArrayList<>(commands.size());
        List<CompletableFuture<Reply>> filterFutures = new ArrayList<>(completableFutureList.size());
        boolean hasBlockingCommands = false;

        for (int i=0; i<commands.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = completableFutureList.get(i);
            ChannelInfo channelInfo = command.getChannelInfo();
            RedisConnection bindClient = channelInfo.getBindClient();
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
                boolean first = false;
                if (bindClient == null) {
                    bindClient = command.getChannelInfo().acquireBindRedisClient(getAddr());
                    channelInfo.setBindClient(bindClient);
                    first = true;
                }
                if (bindClient != null) {
                    CommandTaskQueue asyncTaskQueue = command.getChannelInfo().getAsyncTaskQueue();
                    PubSubUtils.sendByBindClient(bindClient, asyncTaskQueue, command, future, first);
                    byte[][] objects = command.getObjects();
                    if (objects != null && objects.length > 1) {
                        for (int j = 1; j < objects.length; j++) {
                            byte[] channel = objects[j];
                            if (redisCommand == RedisCommand.SUBSCRIBE) {
                                channelInfo.addSubscribeChannels(channel);
                            } else {
                                channelInfo.addPSubscribeChannels(channel);
                            }
                        }
                    }
                } else {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                }
            } else if (redisCommand == RedisCommand.UNSUBSCRIBE || redisCommand == RedisCommand.PUNSUBSCRIBE) {
                if (bindClient != null) {
                    if (command.getObjects() != null && command.getObjects().length > 1) {
                        for (int j = 1; j < command.getObjects().length; j++) {
                            byte[] channel = command.getObjects()[j];
                            if (redisCommand == RedisCommand.UNSUBSCRIBE) {
                                channelInfo.removeSubscribeChannels(channel);
                            } else {
                                channelInfo.removePSubscribeChannels(channel);
                            }
                            if (!channelInfo.hasSubscribeChannels()) {
                                channelInfo.setBindClient(null);
                                bindClient.startIdleCheck();
                            }
                        }
                    }
                    PubSubUtils.sendByBindClient(bindClient, channelInfo.getAsyncTaskQueue(), command, future, false);
                } else {
                    filterCommands.add(command);
                    filterFutures.add(future);
                }
            } else if (redisCommand.getCommandType() == RedisCommand.CommandType.TRANSACTION) {
                if (bindClient == null) {
                    bindClient = command.getChannelInfo().acquireBindRedisClient(getAddr());
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
                        bindClient.startIdleCheck();
                    } else if (redisCommand == RedisCommand.UNWATCH) {
                        if (!channelInfo.isInTransaction()) {
                            channelInfo.setBindClient(null);
                            bindClient.startIdleCheck();
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

    private boolean isPassThroughCommands(List<Command> commands) {
        for (Command command : commands) {
            boolean passThroughCommand = isPassThroughCommand(command);
            if (!passThroughCommand) return false;
        }
        return true;
    }

    private boolean isPassThroughCommand(Command command) {
        RedisConnection bindClient = command.getChannelInfo().getBindClient();
        if (bindClient != null) return false;
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == null) return false;
        return !command.isBlocking() && redisCommand != RedisCommand.SUBSCRIBE && redisCommand != RedisCommand.PSUBSCRIBE
                && redisCommand.getCommandType() != RedisCommand.CommandType.TRANSACTION;
    }

    private void flushBlockingCommands(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        RedisConnectionAddr addr = getAddr();
        if (addr == null) {
            String log = "addr is null, command return NOT_AVAILABLE, RedisResource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisStandaloneClient.class, log);
            }
            return;
        }
        Command lastBlockingCommand = commands.get(commands.size() - 1);
        RedisConnection client = lastBlockingCommand.getChannelInfo().acquireBindRedisClient(addr);
        if (client != null) {
            client.sendCommand(commands, completableFutureList);
            client.startIdleCheck();
        } else {
            String log = "RedisClient[" + PasswordMaskUtils.maskAddr(addr) + "] is null, command return NOT_AVAILABLE, RedisResource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisStandaloneClient.class, log);
            }
        }
    }

    private void flushNoBlockingCommands(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        RedisConnectionAddr addr = getAddr();
        if (addr == null) {
            String log = "addr is null, command return NOT_AVAILABLE, RedisResource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisStandaloneClient.class, log);
            }
            return;
        }
        RedisConnection client = RedisConnectionHub.getInstance().get(addr);
        if (client != null) {
            client.sendCommand(commands, completableFutureList);
        } else {
            String log = "RedisClient[" + PasswordMaskUtils.maskAddr(addr) + "] is null, command return NOT_AVAILABLE, RedisResource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisStandaloneClient.class, log);
            }
        }
    }
}
