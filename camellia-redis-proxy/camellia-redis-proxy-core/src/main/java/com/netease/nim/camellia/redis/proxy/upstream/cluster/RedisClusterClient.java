package com.netease.nim.camellia.redis.proxy.upstream.cluster;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.base.resource.RedisClusterSlavesResource;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.command.CommandTaskQueue;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.CommandFlusher;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionStatus;
import com.netease.nim.camellia.redis.proxy.upstream.utils.CompletableFutureUtils;
import com.netease.nim.camellia.redis.proxy.upstream.utils.PubSubUtils;
import com.netease.nim.camellia.redis.proxy.upstream.utils.ScanCursorCalculator;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2019/12/18.
 */
public class RedisClusterClient implements IUpstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisClusterClient.class);

    private final ScanCursorCalculator cursorCalculator;

    private final int maxAttempts;
    private final RedisClusterSlotInfo clusterSlotInfo;

    private final String url;
    private final String userName;
    private final String password;

    private RedisClusterResource redisClusterResource;
    private RedisClusterSlavesResource redisClusterSlavesResource;

    public RedisClusterClient(RedisClusterSlavesResource redisClusterSlavesResource, int maxAttempts) {
        this.cursorCalculator = new ScanCursorCalculator(ProxyDynamicConf.getInt("redis-cluster.scan.node.bits.len", 10));
        this.redisClusterSlavesResource = redisClusterSlavesResource;
        this.url = redisClusterSlavesResource.getUrl();
        this.userName = redisClusterSlavesResource.getUserName();
        this.password = redisClusterSlavesResource.getPassword();
        this.maxAttempts = maxAttempts;
        this.clusterSlotInfo = new RedisClusterSlotInfo(redisClusterSlavesResource);
        Future<Boolean> future = this.clusterSlotInfo.renew();
        try {
            if (future == null || !future.get()) {
                throw new CamelliaRedisException("RedisClusterSlotInfo init fail");
            }
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaRedisException("RedisClusterSlotInfo init fail", e);
        }
        logger.info("RedisClusterClient init success, resource = {}", redisClusterResource.getUrl());
    }

    public RedisClusterClient(RedisClusterResource redisClusterResource, int maxAttempts) {
        this.cursorCalculator = new ScanCursorCalculator(ProxyDynamicConf.getInt("redis-cluster.scan.node.bits.len", 10));
        this.redisClusterResource = redisClusterResource;
        this.url = redisClusterResource.getUrl();
        this.userName = redisClusterResource.getUserName();
        this.password = redisClusterResource.getPassword();
        this.maxAttempts = maxAttempts;
        this.clusterSlotInfo = new RedisClusterSlotInfo(redisClusterResource);
        Future<Boolean> future = this.clusterSlotInfo.renew();
        try {
            if (future == null || !future.get()) {
                throw new CamelliaRedisException("RedisClusterSlotInfo init fail");
            }
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaRedisException("RedisClusterSlotInfo init fail", e);
        }
        logger.info("RedisClusterClient init success, resource = {}", redisClusterResource.getUrl());
    }

    /**
     * get resource
     * @return resource
     */
    public Resource getResource() {
        if (redisClusterResource != null) return redisClusterResource;
        if (redisClusterSlavesResource != null) return redisClusterSlavesResource;
        return null;
    }

    @Override
    public void preheat() {
        logger.info("try preheat, url = {}", PasswordMaskUtils.maskResource(url));
        Set<RedisClusterSlotInfo.Node> nodes = this.clusterSlotInfo.getNodes();
        for (RedisClusterSlotInfo.Node node : nodes) {
            logger.info("try preheat, url = {}, node = {}", PasswordMaskUtils.maskResource(url), PasswordMaskUtils.maskAddr(node.getAddr()));
            boolean result = RedisConnectionHub.getInstance().preheat(node.getHost(), node.getPort(), node.getUserName(), node.getPassword());
            logger.info("preheat result = {}, url = {}, node = {}", result, PasswordMaskUtils.maskResource(url), PasswordMaskUtils.maskAddr(node.getAddr()));
        }
        logger.info("preheat ok, url = {}", PasswordMaskUtils.maskResource(url));
    }

    @Override
    public String getUrl() {
        return redisClusterResource.getUrl();
    }

    @Override
    public boolean isValid() {
        HashSet<RedisClusterSlotInfo.Node> masterNodes = new HashSet<>(clusterSlotInfo.getMasterSlaveMap().keySet());
        for (RedisClusterSlotInfo.Node master : masterNodes) {
            if (getStatus(master.getAddr()) != RedisConnectionStatus.VALID) {
                return false;
            }
        }
        return true;
    }

    public void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> futureList) {
        if (commands.isEmpty()) return;
        if (commands.size() == 1) {
            Command command = commands.get(0);
            if (isPassThroughCommand(command)) {
                byte[][] args = command.getObjects();
                if (args.length >= 2) {
                    byte[] key = args[1];
                    int slot = RedisClusterCRC16Utils.getSlot(key);
                    RedisConnection connection = getConnection(slot);
                    if (connection != null) {
                        connection.sendCommand(commands, Collections.singletonList(new CompletableFutureWrapper(this, futureList.get(0), command)));
                        if (logger.isDebugEnabled()) {
                            logger.debug("sendCommand, command = {}, key = {}, slot = {}", command.getName(), Utils.bytesToString(key), slot);
                        }
                        return;
                    }
                }
            }
        }

        CommandFlusher commandFlusher = new CommandFlusher(commands.size());
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            ChannelInfo channelInfo = command.getChannelInfo();
            CompletableFuture<Reply> future = futureList.get(i);
            RedisCommand redisCommand = command.getRedisCommand();

            RedisConnection bindConnection = channelInfo.getBindConnection();
            int bindSlot = channelInfo.getBindSlot();

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_1) {
                if (redisCommand.getCommandType() == RedisCommand.CommandType.PUB_SUB) {
                    pubsub(command, future, channelInfo, commandFlusher, redisCommand, bindConnection);
                    continue;
                }
            }

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_2) {
                if (redisCommand.getCommandType() == RedisCommand.CommandType.TRANSACTION) {
                    transaction(command, future, channelInfo, commandFlusher, redisCommand, bindSlot, bindConnection);
                    continue;
                }
            }

            //以multi命令之后的第一个命令归属的slot作为bindSlot
            if (channelInfo.isInTransaction() && bindConnection == null) {
                Command cachedMultiCommand = channelInfo.getCachedMultiCommand();
                if (cachedMultiCommand != null) {
                    List<byte[]> keys = command.getKeys();
                    int slot = RedisClusterCRC16Utils.checkSlot(keys);
                    if (slot < 0) {
                        future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot in TRANSACTION"));
                        continue;
                    }
                    RedisClusterSlotInfo.Node node = clusterSlotInfo.getNode(slot);
                    bindConnection = command.getChannelInfo().acquireBindRedisConnection(node.getAddr());
                    if (bindConnection == null) {
                        future.complete(ErrorReply.NOT_AVAILABLE);
                        continue;
                    }
                    channelInfo.setBindClient(slot, bindConnection);
                    commandFlusher.sendCommand(bindConnection, cachedMultiCommand, new CompletableFuture<>());
                    commandFlusher.sendCommand(bindConnection, command, future);
                    channelInfo.updateCachedMultiCommand(null);
                    continue;
                }
            }

            if (bindConnection != null && bindSlot > 0) {
                commandFlusher.flush();
                commandFlusher.clear();
                List<byte[]> keys = command.getKeys();
                int slot = RedisClusterCRC16Utils.checkSlot(keys);
                if (slot < 0 || slot != bindSlot) {
                    future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot in TRANSACTION"));
                    continue;
                }
                commandFlusher.sendCommand(bindConnection, command, future);
                continue;
            }

            if (redisCommand == RedisCommand.SCAN) {
                scan(commandFlusher, command, future);
                continue;
            }

            if (redisCommand == RedisCommand.SCRIPT) {
                script(commandFlusher, command, future);
                continue;
            }

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.RESTRICTIVE_SUPPORT) {
                List<byte[]> keys = command.getKeys();
                int slot = RedisClusterCRC16Utils.checkSlot(keys);
                if (slot < 0) {
                    future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot"));
                    continue;
                }
                if (command.isBlocking()) {
                    blockingCommand(slot, command, commandFlusher, future);
                } else {
                    RedisConnection connection = getConnection(slot);
                    CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
                    commandFlusher.sendCommand(connection, command, futureWrapper);
                }
                continue;
            }

            if (command.getRedisCommand().getCommandKeyType() != RedisCommand.CommandKeyType.SIMPLE_SINGLE) {
                //这些命令比较特殊，需要把后端的结果merge起来之后再返回给客户端
                boolean continueOk = false;
                switch (redisCommand) {
                    case MGET: {
                        int argLen = command.getObjects().length;
                        int initializerSize = commandFlusher.getInitializerSize();
                        if (argLen > 2) {
                            if (argLen -1 > initializerSize) {
                                commandFlusher.updateInitializerSize(argLen - 1);//调整initializerSize
                            }
                            mget(command, commandFlusher, future);
                            continueOk = true;
                            commandFlusher.updateInitializerSize(initializerSize);
                        }
                        break;
                    }
                    case EXISTS:
                    case UNLINK:
                    case TOUCH:
                    case DEL: {
                        int argLen = command.getObjects().length;
                        int initializerSize = commandFlusher.getInitializerSize();
                        if (argLen > 2) {
                            if (argLen -1 > initializerSize) {
                                commandFlusher.updateInitializerSize(argLen - 1);//调整initializerSize
                            }
                            simpleIntegerReplyMerge(command, commandFlusher, future);
                            continueOk = true;
                            commandFlusher.updateInitializerSize(initializerSize);
                        }
                        break;
                    }
                    case MSET: {
                        int argLen = command.getObjects().length;
                        int keyCount = (argLen - 1) / 2;
                        int initializerSize = commandFlusher.getInitializerSize();
                        if (argLen > 3) {
                            if (keyCount > initializerSize) {
                                commandFlusher.updateInitializerSize(keyCount);//调整initializerSize
                            }
                            mset(command, commandFlusher, future);
                            continueOk = true;
                            commandFlusher.updateInitializerSize(initializerSize);
                        }
                        break;
                    }
                    case JSON_MGET: {
                        int argLen = command.getObjects().length;
                        int keyCount = argLen - 2;
                        if (keyCount > 1) {
                            int initializerSize = commandFlusher.getInitializerSize();
                            if (keyCount > initializerSize) {
                                commandFlusher.updateInitializerSize(keyCount);
                            }
                            jsonMget(command, commandFlusher, future);
                            commandFlusher.updateInitializerSize(initializerSize);
                            continueOk = true;
                        }
                        break;
                    }
                }
                if (continueOk) continue;
            }

            byte[][] args = command.getObjects();
            int slot;
            byte[] key;
            if (redisCommand.getCommandKeyType() == RedisCommand.CommandKeyType.SIMPLE_SINGLE && args.length >= 2) {
                key = args[1];
                slot = RedisClusterCRC16Utils.getSlot(key);
            } else {
                List<byte[]> keys = command.getKeys();
                if (keys.isEmpty()) {
                    key = Utils.EMPTY_ARRAY;
                    slot = ThreadLocalRandom.current().nextInt(RedisClusterSlotInfo.SLOT_SIZE);
                } else {//按道理走到这里的都是只有一个key的命令，且不是blocking的
                    key = keys.get(0);
                    slot = RedisClusterCRC16Utils.getSlot(keys.get(0));
                }
            }
            RedisConnection connection = getConnection(slot);
            if (logger.isDebugEnabled()) {
                logger.debug("sendCommand, command = {}, key = {}, slot = {}", command.getName(), Utils.bytesToString(key), slot);
            }
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
            commandFlusher.sendCommand(connection, command, futureWrapper);
        }
        commandFlusher.flush();
    }

    private void transaction(Command command, CompletableFuture<Reply> future, ChannelInfo channelInfo, CommandFlusher commandFlusher,
                             RedisCommand redisCommand, int bindSlot, RedisConnection bindConnection) {
        if (redisCommand == RedisCommand.WATCH) {
            List<byte[]> keys = command.getKeys();
            if (keys.isEmpty()) {
                future.complete(ErrorReply.argNumWrong(redisCommand));
                return;
            }
            int slot = RedisClusterCRC16Utils.checkSlot(keys);
            if (slot < 0) {
                future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot"));
                return;
            }
            if (bindSlot > 0 && slot != bindSlot) {
                future.complete(new ErrorReply("MULTI WATCH Keys don't hash to the same slot"));
                return;
            }
            if (bindConnection == null) {
                RedisClusterSlotInfo.Node node = clusterSlotInfo.getNode(slot);
                bindConnection = command.getChannelInfo().acquireBindRedisConnection(node.getAddr());
                channelInfo.setBindClient(slot, bindConnection);
            }
            commandFlusher.sendCommand(bindConnection, command, future);
        } else if (redisCommand == RedisCommand.UNWATCH) {
            if (bindConnection != null) {
                commandFlusher.sendCommand(bindConnection, command, future);
                if (!channelInfo.isInTransaction()) {
                    channelInfo.setBindClient(-1, null);
                    bindConnection.startIdleCheck();
                }
            } else {
                future.complete(StatusReply.OK);
            }
        } else if (redisCommand == RedisCommand.MULTI) {
            if (channelInfo.isInTransaction()) {
                future.complete(new ErrorReply("ERR MULTI calls can not be nested"));
                return;
            }
            channelInfo.setInTransaction(true);
            if (bindConnection != null) {
                commandFlusher.sendCommand(bindConnection, command, future);
            } else {
                channelInfo.updateCachedMultiCommand(command);
                future.complete(StatusReply.OK);
            }
        } else if (redisCommand == RedisCommand.EXEC || redisCommand == RedisCommand.DISCARD) {
            if (!channelInfo.isInTransaction()) {
                future.complete(new ErrorReply("ERR " + redisCommand.strRaw() + " without MULTI"));
                return;
            }
            if (bindConnection != null) {
                commandFlusher.sendCommand(bindConnection, command, future);
                bindConnection.startIdleCheck();
            } else {
                future.complete(new ErrorReply("ERR " + redisCommand.strRaw() + " without MULTI"));
            }
            channelInfo.setBindClient(-1, null);
            channelInfo.setInTransaction(false);
        } else {
            future.complete(ErrorReply.NOT_SUPPORT);
        }
    }

    private void pubsub(Command command, CompletableFuture<Reply> future, ChannelInfo channelInfo, CommandFlusher commandFlusher,
                             RedisCommand redisCommand, RedisConnection bindConnection) {
        if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
            boolean first = false;
            if (bindConnection == null) {
                int randomSlot = ThreadLocalRandom.current().nextInt(RedisClusterSlotInfo.SLOT_SIZE);
                RedisClusterSlotInfo.Node node = clusterSlotInfo.getNode(randomSlot);
                if (node == null) {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                    return;
                }
                bindConnection = command.getChannelInfo().acquireBindRedisConnection(node.getAddr());
                channelInfo.setBindConnection(bindConnection);
                first = true;
            }
            if (bindConnection != null) {
                CommandTaskQueue taskQueue = channelInfo.getCommandTaskQueue();
                commandFlusher.flush();
                commandFlusher.clear();
                PubSubUtils.sendByBindClient(bindConnection, taskQueue, command, future, first);
                byte[][] objects = command.getObjects();
                if (objects != null && objects.length > 1) {
                    for (int j = 1; j < objects.length; j++) {
                        byte[] channel = objects[j];
                        if (redisCommand == RedisCommand.SUBSCRIBE) {
                            command.getChannelInfo().addSubscribeChannels(channel);
                        } else {
                            command.getChannelInfo().addPSubscribeChannels(channel);
                        }
                    }
                }
            } else {
                future.complete(ErrorReply.NOT_AVAILABLE);
            }
            return;
        }
        if (bindConnection != null && (redisCommand == RedisCommand.UNSUBSCRIBE || redisCommand == RedisCommand.PUNSUBSCRIBE)) {
            byte[][] objects = command.getObjects();
            if (objects != null && objects.length > 1) {
                for (int j = 1; j < objects.length; j++) {
                    byte[] channel = objects[j];
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
        }

        if (bindConnection != null) {
            commandFlusher.flush();
            commandFlusher.clear();
            PubSubUtils.sendByBindClient(bindConnection, command.getChannelInfo().getCommandTaskQueue(), command, future, false);
        } else {
            RedisConnection connection = getConnection(ThreadLocalRandom.current().nextInt(RedisClusterSlotInfo.SLOT_SIZE));
            if (connection != null) {
                commandFlusher.sendCommand(connection, command, new CompletableFutureWrapper(this, future, command));
            } else {
                future.complete(ErrorReply.NOT_AVAILABLE);
            }
        }
    }

    private void script(CommandFlusher commandFlusher, Command command, CompletableFuture<Reply> future) {
        byte[][] objects = command.getObjects();
        if (objects.length <= 1) {
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            return;
        }
        String ope = Utils.bytesToString(objects[1]);
        if (ope.equalsIgnoreCase(RedisKeyword.FLUSH.name()) || ope.equalsIgnoreCase(RedisKeyword.LOAD.name())) {
            Set<RedisClusterSlotInfo.Node> nodes = clusterSlotInfo.getNodes();
            boolean futureSetting = false;
            for (RedisClusterSlotInfo.Node node : nodes) {
                RedisConnection redisConnection = RedisConnectionHub.getInstance().get(node.getAddr());
                //只返回第一个reply
                commandFlusher.sendCommand(redisConnection, command, !futureSetting ? future : new CompletableFuture<>());
                futureSetting = true;
            }
        } else if (ope.equalsIgnoreCase(RedisKeyword.EXISTS.name())) {
            Set<RedisClusterSlotInfo.Node> nodes = clusterSlotInfo.getNodes();
            List<CompletableFuture<Reply>> futures = new ArrayList<>();
            for (RedisClusterSlotInfo.Node node : nodes) {
                RedisConnection redisConnection = RedisConnectionHub.getInstance().get(node.getAddr());
                CompletableFuture<Reply> f = new CompletableFuture<>();
                commandFlusher.sendCommand(redisConnection, command, f);
                futures.add(f);
            }
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeMultiIntegerReply(replies)));
        } else {
            future.complete(ErrorReply.NOT_SUPPORT);
        }
    }

    private void scan(CommandFlusher commandFlusher, Command command, CompletableFuture<Reply> future) {
        byte[][] objects = command.getObjects();
        if (objects == null || objects.length <= 1) {
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            return;
        }

        int currentNodeIndex = cursorCalculator.filterScanCommand(command);
        if (currentNodeIndex < 0) {
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            return;
        }

        if (currentNodeIndex >= clusterSlotInfo.getNodesSize()) {
            future.complete(new ErrorReply("ERR illegal arguments of cursor"));
            return;
        }

        RedisConnection redisConnection = clusterSlotInfo.getConnectionByIndex(currentNodeIndex);
        if (redisConnection == null || !redisConnection.isValid()) {
            future.complete(ErrorReply.NOT_AVAILABLE);
            return;
        }

        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        completableFuture.thenApply((reply) -> cursorCalculator.filterScanReply(reply, currentNodeIndex, clusterSlotInfo.getNodesSize())).thenAccept(future::complete);
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, completableFuture, command);
        commandFlusher.sendCommand(redisConnection, command, futureWrapper);
    }

    private RedisConnection getConnection(int slot) {
        RedisConnection connection = null;
        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts++;
            connection = clusterSlotInfo.getConnection(slot);
            if (connection != null && connection.isValid()) {
                break;
            } else {
                clusterSlotInfo.renew();
            }
        }
        return connection;
    }

    private static class CompletableFutureWrapper extends CompletableFuture<Reply> {
        private static final Command ASKING = new Command(new byte[][]{RedisCommand.ASKING.raw()});
        private final RedisClusterClient clusterClient;
        private final CompletableFuture<Reply> future;
        private final Command command;
        private int attempts = 0;

        CompletableFutureWrapper(RedisClusterClient clusterClient, CompletableFuture<Reply> future, Command command) {
            this.clusterClient = clusterClient;
            this.future = future;
            this.command = command;
        }

        public boolean complete(Reply reply) {
            try {
                if (attempts < clusterClient.maxAttempts) {
                    if (reply instanceof ErrorReply) {
                        String error = ((ErrorReply) reply).getError();
                        if (error.startsWith("MOVED")) {
                            attempts++;
                            String log = "MOVED, command = " + command.getName() + ", attempts = " + attempts;
                            ErrorLogCollector.collect(RedisClusterClient.class, log);
                            clusterClient.clusterSlotInfo.renew();
                            String[] strings = parseTargetHostAndSlot(error);
                            RedisConnectionAddr addr = new RedisConnectionAddr(strings[1], Integer.parseInt(strings[2]), clusterClient.userName, clusterClient.password);
                            if (command.isBlocking()) {
                                RedisConnection redisConnection = command.getChannelInfo().tryAcquireBindRedisConnection(addr);
                                if (redisConnection != null && redisConnection.isValid()) {
                                    ErrorLogCollector.collect(RedisClusterClient.class,
                                            "MOVED, [BlockingCommand] [RedisConnection tryAcquireBindRedisConnection success], command = " + command.getName() + ", attempts = " + attempts);
                                    redisConnection.sendCommand(Collections.singletonList(command), Collections.singletonList(this));
                                    redisConnection.startIdleCheck();
                                } else {
                                    RedisConnection connection = RedisConnectionHub.getInstance().newConnection(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword());
                                    try {
                                        if (connection == null || !connection.isValid()) {
                                            ErrorLogCollector.collect(RedisClusterClient.class,
                                                    "MOVED, [BlockingCommand] [RedisConnection newConnection fail], command = " + command.getName() + ", attempts = " + attempts);
                                            clusterClient.clusterSlotInfo.renew();
                                            CompletableFutureWrapper.this.future.complete(reply);
                                        } else {
                                            ErrorLogCollector.collect(RedisClusterClient.class,
                                                    "MOVED, [BlockingCommand] [RedisConnection newConnection success], command = " + command.getName() + ", attempts = " + attempts);
                                            connection.sendCommand(Collections.singletonList(command), Collections.singletonList(CompletableFutureWrapper.this));
                                            connection.startIdleCheck();
                                            command.getChannelInfo().updateBindRedisConnectionCache(connection);
                                        }
                                    } catch (Exception e) {
                                        ErrorLogCollector.collect(RedisClusterClient.class,
                                                "MOVED, [BlockingCommand] [RedisConnection newConnection error], command = " + command.getName() + ", attempts = " + attempts, e);
                                        CompletableFutureWrapper.this.future.complete(reply);
                                    }
                                }
                            } else {
                                RedisConnection connection = RedisConnectionHub.getInstance().get(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword());
                                try {
                                    if (connection == null || !connection.isValid()) {
                                        ErrorLogCollector.collect(RedisClusterClient.class,
                                                "MOVED, [RedisConnection get fail], command = " + command.getName() + ", attempts = " + attempts);
                                        clusterClient.clusterSlotInfo.renew();
                                        CompletableFutureWrapper.this.future.complete(reply);
                                    } else {
                                        ErrorLogCollector.collect(RedisClusterClient.class,
                                                "MOVED, [RedisConnection get success], command = " + command.getName() + ", attempts = " + attempts);
                                        connection.sendCommand(Collections.singletonList(command), Collections.singletonList(CompletableFutureWrapper.this));
                                    }
                                } catch (Exception e) {
                                    ErrorLogCollector.collect(RedisClusterClient.class,
                                            "MOVED, [RedisConnection get error], command = " + command.getName() + ", attempts = " + attempts, e);
                                    CompletableFutureWrapper.this.future.complete(reply);
                                }
                            }
                            return true;
                        } else if (error.startsWith("ASK")) {
                            attempts++;
                            String log = "ASK, command = " + command.getName() + ", attempts = " + attempts;
                            ErrorLogCollector.collect(RedisClusterClient.class, log);
                            String[] strings = parseTargetHostAndSlot(error);
                            RedisConnectionAddr addr = new RedisConnectionAddr(strings[1], Integer.parseInt(strings[2]), clusterClient.userName, clusterClient.password);
                            if (command.isBlocking()) {
                                RedisConnection redisConnection = command.getChannelInfo().tryAcquireBindRedisConnection(addr);
                                if (redisConnection != null && redisConnection.isValid()) {
                                    ErrorLogCollector.collect(RedisClusterClient.class,
                                            "ASK, [BlockingCommand] [RedisConnection tryAcquireBindRedisConnection success], command = " + command.getName() + ", attempts = " + attempts);
                                    redisConnection.sendCommand(Arrays.asList(ASKING, command), Arrays.asList(new CompletableFuture<>(), this));
                                    redisConnection.startIdleCheck();
                                } else {
                                    RedisConnection connection = RedisConnectionHub.getInstance().newConnection(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword());
                                    try {
                                        if (connection == null || !connection.isValid()) {
                                            ErrorLogCollector.collect(RedisClusterClient.class,
                                                    "ASK, [BlockingCommand] [RedisConnection newConnection fail], command = " + command.getName() + ", attempts = " + attempts);
                                            clusterClient.clusterSlotInfo.renew();
                                            CompletableFutureWrapper.this.future.complete(reply);
                                        } else {
                                            ErrorLogCollector.collect(RedisClusterClient.class,
                                                    "ASK, [BlockingCommand] [RedisConnection newConnection success], command = " + command.getName() + ", attempts = " + attempts);
                                            connection.sendCommand(Arrays.asList(ASKING, command), Arrays.asList(new CompletableFuture<>(), CompletableFutureWrapper.this));
                                            connection.startIdleCheck();
                                            command.getChannelInfo().updateBindRedisConnectionCache(connection);
                                        }
                                    } catch (Exception e) {
                                        ErrorLogCollector.collect(RedisClusterClient.class,
                                                "ASK, [BlockingCommand] [RedisConnection newConnection error], command = " + command.getName() + ", attempts = " + attempts, e);
                                        CompletableFutureWrapper.this.future.complete(reply);
                                    }
                                }
                            } else {
                                RedisConnection connection = RedisConnectionHub.getInstance().get(strings[1], Integer.parseInt(strings[2]), clusterClient.userName, clusterClient.password);
                                try {
                                    if (connection == null || !connection.isValid()) {
                                        ErrorLogCollector.collect(RedisClusterClient.class,
                                                "ASK, [RedisConnection get fail], command = " + command.getName() + ", attempts = " + attempts);
                                        clusterClient.clusterSlotInfo.renew();
                                        CompletableFutureWrapper.this.future.complete(reply);
                                    } else {
                                        ErrorLogCollector.collect(RedisClusterClient.class,
                                                "ASK, [RedisConnection get success], command = " + command.getName() + ", attempts = " + attempts);
                                        connection.sendCommand(Arrays.asList(ASKING, command), Arrays.asList(new CompletableFuture<>(), CompletableFutureWrapper.this));
                                    }
                                } catch (Exception e) {
                                    ErrorLogCollector.collect(RedisClusterClient.class,
                                            "ASK, [RedisConnection get error], command = " + command.getName() + ", attempts = " + attempts, e);
                                    CompletableFutureWrapper.this.future.complete(reply);
                                }
                            }
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("CompletableFutureWrapper complete error, command = {}, errorReply = {}",
                        command.getName(), ((ErrorReply) reply).getError(), e);
            }
            future.complete(reply);
            return true;
        }

        private static String[] parseTargetHostAndSlot(String clusterRedirectResponse) {
            String[] response = new String[3];
            String[] messageInfo = clusterRedirectResponse.split(" ");
            String[] targetHostAndPort = extractParts(messageInfo[2]);
            response[0] = messageInfo[1];
            response[1] = targetHostAndPort[0];
            response[2] = targetHostAndPort[1];
            return response;
        }

        private static String[] extractParts(String from) {
            int idx = from.lastIndexOf(":");
            String host = idx != -1 ? from.substring(0, idx) : from;
            String port = idx != -1 ? from.substring(idx + 1) : "";
            return new String[]{host, port};
        }
    }

    private boolean isPassThroughCommand(Command command) {
        ChannelInfo channelInfo = command.getChannelInfo();
        if (channelInfo.isInTransaction()) {
            return false;
        }
        RedisConnection bindConnection = channelInfo.getBindConnection();
        if (bindConnection != null) return false;
        RedisCommand redisCommand = command.getRedisCommand();
        RedisCommand.CommandSupportType supportType = redisCommand.getSupportType();
        if (supportType == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_2
                || supportType == RedisCommand.CommandSupportType.RESTRICTIVE_SUPPORT) {
            return false;
        }
        RedisCommand.CommandKeyType commandKeyType = redisCommand.getCommandKeyType();
        return commandKeyType == RedisCommand.CommandKeyType.SIMPLE_SINGLE && !command.isBlocking();
    }

    private void jsonMget(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] args = command.getObjects();
        List<CompletableFuture<Reply>> futureList = new ArrayList<>();

        byte[] path = args[args.length - 1];
        for (int i = 1; i < args.length - 1; i++) {
            byte[] key = args[i];
            int slot = RedisClusterCRC16Utils.getSlot(key);
            RedisConnection connection = getConnection(slot);
            Command subCommand = new Command(new byte[][]{RedisCommand.JSON_MGET.raw(), key, path});
            CompletableFuture<Reply> subFuture = new CompletableFuture<>();
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, subFuture, subCommand);
            commandFlusher.sendCommand(connection, subCommand, futureWrapper);
            futureList.add(subFuture);
        }
        if (futureList.size() == 1) {
            CompletableFuture<Reply> completableFuture = futureList.get(0);
            completableFuture.thenAccept(reply -> {
                if (reply instanceof ErrorReply) {
                    future.complete(reply);
                } else {
                    future.complete(new MultiBulkReply(new Reply[]{reply}));
                }
            });
            return;
        }
        CompletableFutureUtils.allOf(futureList).thenAccept(replies -> {
            Reply[] retRelies = new Reply[replies.size()];
            for (int i = 0; i < replies.size(); i++) {
                retRelies[i] = replies.get(i);
                if (retRelies[i] instanceof ErrorReply) {
                    future.complete(retRelies[i]);
                    return;
                }
            }
            future.complete(new MultiBulkReply(retRelies));
        });
    }

    private void mget(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] args = command.getObjects();
        List<CompletableFuture<Reply>> futureList = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            byte[] key = args[i];
            int slot = RedisClusterCRC16Utils.getSlot(key);
            RedisConnection connection = getConnection(slot);
            Command subCommand = new Command(new byte[][]{RedisCommand.GET.raw(), key});

            CompletableFuture<Reply> subFuture = new CompletableFuture<>();
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, subFuture, subCommand);
            commandFlusher.sendCommand(connection, subCommand, futureWrapper);
            futureList.add(subFuture);
        }
        if (futureList.size() == 1) {
            CompletableFuture<Reply> completableFuture = futureList.get(0);
            completableFuture.thenAccept(reply -> {
                if (reply instanceof ErrorReply) {
                    future.complete(reply);
                } else {
                    future.complete(new MultiBulkReply(new Reply[]{reply}));
                }
            });
            return;
        }
        CompletableFutureUtils.allOf(futureList).thenAccept(replies -> {
            Reply[] retRelies = new Reply[replies.size()];
            for (int i = 0; i < replies.size(); i++) {
                retRelies[i] = replies.get(i);
                if (retRelies[i] instanceof ErrorReply) {
                    future.complete(retRelies[i]);
                    return;
                }
            }
            future.complete(new MultiBulkReply(retRelies));
        });
    }

    private void mset(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] args = command.getObjects();
        if ((args.length - 1) % 2 != 0) {
            future.complete(new ErrorReply("wrong number of arguments for 'mset' command"));
            return;
        }
        List<CompletableFuture<Reply>> futureList = new ArrayList<>();
        for (int i = 1; i < args.length; i++, i++) {
            byte[] key = args[i];
            byte[] value = args[i + 1];
            int slot = RedisClusterCRC16Utils.getSlot(key);
            RedisConnection connection = getConnection(slot);
            Command subCommand = new Command(new byte[][]{RedisCommand.SET.raw(), key, value});

            CompletableFuture<Reply> subFuture = new CompletableFuture<>();
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, subFuture, subCommand);
            commandFlusher.sendCommand(connection, subCommand, futureWrapper);
            futureList.add(subFuture);
        }
        if (futureList.size() == 1) {
            CompletableFuture<Reply> completableFuture = futureList.get(0);
            completableFuture.thenAccept(future::complete);
            return;
        }
        CompletableFutureUtils.allOf(futureList).thenAccept(replies -> future.complete(Utils.mergeStatusReply(replies)));
    }

    private void simpleIntegerReplyMerge(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] args = command.getObjects();
        List<CompletableFuture<Reply>> futureList = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            byte[] key = args[i];
            int slot = RedisClusterCRC16Utils.getSlot(key);
            RedisConnection connection = getConnection(slot);
            Command subCommand = new Command(new byte[][]{args[0], key});

            CompletableFuture<Reply> subFuture = new CompletableFuture<>();
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, subFuture, subCommand);
            commandFlusher.sendCommand(connection, subCommand, futureWrapper);
            futureList.add(subFuture);
        }
        if (futureList.size() == 1) {
            CompletableFuture<Reply> completableFuture = futureList.get(0);
            completableFuture.thenAccept(future::complete);
            return;
        }
        CompletableFutureUtils.allOf(futureList).thenAccept(replies -> future.complete(Utils.mergeIntegerReply(replies)));
    }

    private void blockingCommand(int slot, Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        if (slot < 0) {
            future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot"));
            return;
        }
        RedisClusterSlotInfo.Node node = clusterSlotInfo.getNode(slot);
        if (node == null) {
            ErrorLogCollector.collect(RedisClusterClient.class, "blockingCommand getNode, slot=" + slot + " fail");
            future.complete(ErrorReply.NOT_AVAILABLE);
            return;
        }
        RedisConnection connection = command.getChannelInfo().acquireBindRedisConnection(node.getAddr());
        if (connection == null || !connection.isValid()) {
            ErrorLogCollector.collect(RedisClusterClient.class, "blockingCommand newClient, node=" + node.getAddr() + " fail");
            future.complete(ErrorReply.NOT_AVAILABLE);
            return;
        }
        commandFlusher.flush();
        commandFlusher.clear();
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
        connection.sendCommand(Collections.singletonList(command), Collections.singletonList(futureWrapper));
        connection.startIdleCheck();
    }

}
