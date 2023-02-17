package com.netease.nim.camellia.redis.proxy.upstream.standalone;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
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
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
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

    private final ConcurrentLinkedHashMap<String, RedisConnectionAddr[]> cache = new ConcurrentLinkedHashMap.Builder<String, RedisConnectionAddr[]>()
            .initialCapacity(128).maximumWeightedCapacity(1024).build();

    public abstract RedisConnectionAddr getAddr();
    public abstract Resource getResource();

    private RedisConnectionAddr getAddr(int db) {
        RedisConnectionAddr addr = getAddr();
        if (addr == null) {
            return null;
        }
        if (db < 0 || db == addr.getDb()) {
            return addr;
        }
        RedisConnectionAddr[] addrs = CamelliaMapUtils.computeIfAbsent(cache, addr.getUrl(), k -> new RedisConnectionAddr[16]);
        if (db < addrs.length) {
            RedisConnectionAddr cacheAddr = addrs[db];
            if (cacheAddr == null) {
                cacheAddr = new RedisConnectionAddr(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword(), db);
                addrs[db] = cacheAddr;
            }
            return cacheAddr;
        }
        return new RedisConnectionAddr(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword(), db);
    }

    @Override
    public String getUrl() {
        return getResource().getUrl();
    }

    @Override
    public void preheat() {
        if (logger.isInfoEnabled()) {
            logger.info("try preheat, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
        }
        RedisConnectionAddr addr = getAddr();
        boolean result = RedisConnectionHub.getInstance().preheat(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword(), addr.getDb());
        if (logger.isInfoEnabled()) {
            logger.info("preheat result = {}, url = {}", result, PasswordMaskUtils.maskResource(getResource().getUrl()));
        }
    }

    public void sendCommand(int db, List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        if (commands.size() == 1) {
            Command command = commands.get(0);
            if (isPassThroughCommand(command)) {
                flushNoBlockingCommands(db, commands, completableFutureList);
                return;
            }
        } else {
            if (isPassThroughCommands(commands)) {
                flushNoBlockingCommands(db, commands, completableFutureList);
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
            RedisConnection bindConnection = channelInfo.getBindConnection();
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
                boolean first = false;
                if (bindConnection == null) {
                    bindConnection = command.getChannelInfo().acquireBindRedisConnection(getAddr(db));
                    channelInfo.setBindConnection(bindConnection);
                    first = true;
                }
                if (bindConnection != null) {
                    CommandTaskQueue taskQueue = command.getChannelInfo().getCommandTaskQueue();
                    PubSubUtils.sendByBindClient(bindConnection, taskQueue, command, future, first);
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
                if (bindConnection != null) {
                    if (command.getObjects() != null && command.getObjects().length > 1) {
                        for (int j = 1; j < command.getObjects().length; j++) {
                            byte[] channel = command.getObjects()[j];
                            if (redisCommand == RedisCommand.UNSUBSCRIBE) {
                                channelInfo.removeSubscribeChannels(channel);
                            } else {
                                channelInfo.removePSubscribeChannels(channel);
                            }
                            if (!channelInfo.hasSubscribeChannels()) {
                                channelInfo.setBindConnection(null);
                                bindConnection.startIdleCheck();
                            }
                        }
                    }
                    PubSubUtils.sendByBindClient(bindConnection, channelInfo.getCommandTaskQueue(), command, future, false);
                } else {
                    filterCommands.add(command);
                    filterFutures.add(future);
                }
            } else if (redisCommand.getCommandType() == RedisCommand.CommandType.TRANSACTION) {
                if (bindConnection == null) {
                    bindConnection = command.getChannelInfo().acquireBindRedisConnection(getAddr(db));
                    channelInfo.setBindConnection(bindConnection);
                }
                if (bindConnection == null) {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                } else {
                    bindConnection.sendCommand(Collections.singletonList(command), Collections.singletonList(future));
                    if (redisCommand == RedisCommand.MULTI) {
                        channelInfo.setInTransaction(true);
                    } else if (redisCommand == RedisCommand.EXEC || redisCommand == RedisCommand.DISCARD) {
                        channelInfo.setInTransaction(false);
                        channelInfo.setBindConnection(null);
                        bindConnection.startIdleCheck();
                    } else if (redisCommand == RedisCommand.UNWATCH) {
                        if (!channelInfo.isInTransaction()) {
                            channelInfo.setBindConnection(null);
                            bindConnection.startIdleCheck();
                        }
                    }
                }
            } else {
                if (bindConnection != null) {
                    bindConnection.sendCommand(Collections.singletonList(command), Collections.singletonList(future));
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
            flushNoBlockingCommands(db, commands, completableFutureList);
            return;
        }

        if (commands.size() == 1) {
            flushBlockingCommands(db, commands, completableFutureList);
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
                flushBlockingCommands(db, commands1, completableFutureList1);
                commands1 = new ArrayList<>(commands.size());
                completableFutureList1 = new ArrayList<>(commands.size());
            }
        }
        if (!commands1.isEmpty()) {
            flushNoBlockingCommands(db, commands1, completableFutureList1);
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
        RedisConnection bindConnection = command.getChannelInfo().getBindConnection();
        if (bindConnection != null) return false;
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == null) return false;
        return !command.isBlocking() && redisCommand != RedisCommand.SUBSCRIBE && redisCommand != RedisCommand.PSUBSCRIBE
                && redisCommand.getCommandType() != RedisCommand.CommandType.TRANSACTION;
    }

    private void flushBlockingCommands(int db, List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        RedisConnectionAddr addr = getAddr(db);
        if (addr == null) {
            String log = "addr is null, command return NOT_AVAILABLE, RedisResource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisStandaloneClient.class, log);
            }
            return;
        }
        Command lastBlockingCommand = commands.get(commands.size() - 1);
        RedisConnection connection = lastBlockingCommand.getChannelInfo().acquireBindRedisConnection(addr);
        if (connection != null) {
            connection.sendCommand(commands, completableFutureList);
            connection.startIdleCheck();
        } else {
            String log = "RedisClient[" + PasswordMaskUtils.maskAddr(addr) + "] is null, command return NOT_AVAILABLE, RedisResource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisStandaloneClient.class, log);
            }
        }
    }

    private void flushNoBlockingCommands(int db, List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        RedisConnectionAddr addr = getAddr(db);
        if (addr == null) {
            String log = "addr is null, command return NOT_AVAILABLE, RedisResource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisStandaloneClient.class, log);
            }
            return;
        }
        RedisConnection connection = RedisConnectionHub.getInstance().get(addr);
        if (connection != null) {
            connection.sendCommand(commands, completableFutureList);
        } else {
            String log = "RedisClient[" + PasswordMaskUtils.maskAddr(addr) + "] is null, command return NOT_AVAILABLE, RedisResource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisStandaloneClient.class, log);
            }
        }
    }
}
