package com.netease.nim.camellia.redis.proxy.upstream.standalone;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
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

    private final ConcurrentLinkedHashMap<String, RedisConnectionAddr> cache = new ConcurrentLinkedHashMap.Builder<String, RedisConnectionAddr>()
            .initialCapacity(128).maximumWeightedCapacity(10240).build();

    public abstract RedisConnectionAddr getAddr();
    public abstract Resource getResource();

    private RedisConnectionAddr getAddr(int db) {
        RedisConnectionAddr addr = getAddr();
        if (addr == null) {
            renew();
            return null;
        }
        if (db < 0 || db == addr.getDb()) {
            return addr;
        }
        String key = addr.getUrl() + "|" + db;
        RedisConnectionAddr target = cache.get(key);
        if (target == null) {
            target = cache.computeIfAbsent(key, s -> new RedisConnectionAddr(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword(), db));
        }
        return target;
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
        boolean result = RedisConnectionHub.getInstance().preheat(this, addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword(), addr.getDb());
        if (logger.isInfoEnabled()) {
            logger.info("preheat result = {}, url = {}", result, PasswordMaskUtils.maskResource(getResource().getUrl()));
        }
    }

    @Override
    public void shutdown() {
        //do nothing
        logger.warn("upstream client shutdown, url = {}", getUrl());
    }

    public void sendCommand(int db, List<Command> commands, List<CompletableFuture<Reply>> futureList) {
        if (logger.isDebugEnabled()) {
            List<String> commandNames = new ArrayList<>();
            for (Command command : commands) {
                commandNames.add(command.getName());
            }
            logger.debug("receive commands, url = {}, db = {}, commands = {}", getUrl(), db, commandNames);
        }
        if (commands.size() == 1) {
            Command command = commands.get(0);
            if (isPassThroughCommand(command)) {
                if (command.getRedisCommand() == RedisCommand.PING) {
                    futureList.get(0).complete(StatusReply.PONG);
                    return;
                }
                flushNoBlockingCommands(db, commands, futureList);
                return;
            }
        } else {
            if (isPassThroughCommands(commands)) {
                flushNoBlockingCommands(db, commands, futureList);
                return;
            }
        }
        List<Command> filterCommands = new ArrayList<>(commands.size());
        List<CompletableFuture<Reply>> filterFutures = new ArrayList<>(futureList.size());
        boolean hasBlockingCommands = false;

        for (int i=0; i<commands.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = futureList.get(i);
            ChannelInfo channelInfo = command.getChannelInfo();
            RedisConnection bindConnection = channelInfo.getBindConnection();
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand == RedisCommand.PING) {
                if (bindConnection == null) {
                    future.complete(StatusReply.PONG);
                    continue;
                }
            }

            if (channelInfo.isInSubscribe() && bindConnection != null && redisCommand.getCommandType() != RedisCommand.CommandType.PUB_SUB) {
                if (!filterCommands.isEmpty()) {
                    flush(db, filterCommands, filterFutures, hasBlockingCommands);
                    filterCommands = new ArrayList<>();
                    filterFutures = new ArrayList<>();
                    hasBlockingCommands = false;
                }
                CommandTaskQueue taskQueue = command.getChannelInfo().getCommandTaskQueue();
                PubSubUtils.sendByBindClient(getResource(), bindConnection, taskQueue, command, future, false);
                continue;
            }

            if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
                boolean first = false;
                if (bindConnection == null) {
                    bindConnection = command.getChannelInfo().acquireBindSubscribeRedisConnection(this, getAddr(db));
                    channelInfo.setBindConnection(bindConnection);
                    first = true;
                }
                if (bindConnection != null) {
                    if (!filterCommands.isEmpty()) {
                        flush(db, filterCommands, filterFutures, hasBlockingCommands);
                        filterCommands = new ArrayList<>();
                        filterFutures = new ArrayList<>();
                        hasBlockingCommands = false;
                    }
                    CommandTaskQueue taskQueue = command.getChannelInfo().getCommandTaskQueue();
                    PubSubUtils.sendByBindClient(getResource(), bindConnection, taskQueue, command, future, first);
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
                    future.complete(ErrorReply.UPSTREAM_BIND_CONNECTION_NULL);
                    renew();
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
                    if (!filterCommands.isEmpty()) {
                        flush(db, filterCommands, filterFutures, hasBlockingCommands);
                        filterCommands = new ArrayList<>();
                        filterFutures = new ArrayList<>();
                        hasBlockingCommands = false;
                    }
                    PubSubUtils.sendByBindClient(getResource(), bindConnection, channelInfo.getCommandTaskQueue(), command, future, false);
                } else {
                    filterCommands.add(command);
                    filterFutures.add(future);
                }
            } else if (redisCommand.getCommandType() == RedisCommand.CommandType.TRANSACTION) {
                if (bindConnection == null) {
                    bindConnection = command.getChannelInfo().acquireBindRedisConnection(this, getAddr(db));
                    channelInfo.setBindConnection(bindConnection);
                }
                if (bindConnection == null) {
                    future.complete(ErrorReply.UPSTREAM_BIND_CONNECTION_NULL);
                    renew();
                } else {
                    if (!filterCommands.isEmpty()) {
                        flush(db, filterCommands, filterFutures, hasBlockingCommands);
                        filterCommands = new ArrayList<>();
                        filterFutures = new ArrayList<>();
                        hasBlockingCommands = false;
                    }
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
                    if (!filterCommands.isEmpty()) {
                        flush(db, filterCommands, filterFutures, hasBlockingCommands);
                        filterCommands = new ArrayList<>();
                        filterFutures = new ArrayList<>();
                        hasBlockingCommands = false;
                    }
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
        flush(db, filterCommands, filterFutures, hasBlockingCommands);
    }

    private void flush(int db, List<Command> commands, List<CompletableFuture<Reply>> futureList, boolean hasBlockingCommands) {
        if (!hasBlockingCommands) {
            flushNoBlockingCommands(db, commands, futureList);
            return;
        }

        if (commands.size() == 1) {
            flushBlockingCommands(db, commands, futureList);
            return;
        }

        List<Command> commands1 = new ArrayList<>(commands.size());
        List<CompletableFuture<Reply>> completableFutureList1 = new ArrayList<>(commands.size());
        for (int i=0; i<commands.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = futureList.get(i);
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
            String log = "addr is null, command return NOT_AVAILABLE, resource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.UPSTREAM_CONNECTION_REDIS_NODE_NULL);
                ErrorLogCollector.collect(AbstractSimpleRedisClient.class, log);
            }
            return;
        }
        Command lastBlockingCommand = commands.get(commands.size() - 1);
        RedisConnection connection = lastBlockingCommand.getChannelInfo().acquireBindRedisConnection(this, addr);
        if (connection != null) {
            connection.sendCommand(commands, completableFutureList);
            connection.startIdleCheck();
        } else {
            renew();
            String log = "RedisConnection[" + PasswordMaskUtils.maskAddr(addr) + "] is null, command return NOT_AVAILABLE, resource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.UPSTREAM_BIND_CONNECTION_NULL);
                ErrorLogCollector.collect(AbstractSimpleRedisClient.class, log);
            }
        }
    }

    private void flushNoBlockingCommands(int db, List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        RedisConnectionAddr addr = getAddr(db);
        if (addr == null) {
            String log = "addr is null, command return NOT_AVAILABLE, resource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.UPSTREAM_CONNECTION_REDIS_NODE_NULL);
                ErrorLogCollector.collect(AbstractSimpleRedisClient.class, log);
            }
            return;
        }
        RedisConnection connection = RedisConnectionHub.getInstance().get(this, addr);
        if (connection != null) {
            connection.sendCommand(commands, completableFutureList);
        } else {
            renew();
            String log = "RedisConnection[" + PasswordMaskUtils.maskAddr(addr) + "] is null, command return NOT_AVAILABLE, resource = " + PasswordMaskUtils.maskResource(getResource().getUrl());
            for (CompletableFuture<Reply> completableFuture : completableFutureList) {
                completableFuture.complete(ErrorReply.UPSTREAM_CONNECTION_NULL);
                ErrorLogCollector.collect(AbstractSimpleRedisClient.class, log);
            }
        }
    }

    @Override
    public void renew() {
        //do nothing
    }
}
