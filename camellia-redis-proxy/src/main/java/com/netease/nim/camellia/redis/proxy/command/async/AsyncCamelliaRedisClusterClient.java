package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.redis.resource.RedisClusterResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2019/12/18.
 */
public class AsyncCamelliaRedisClusterClient implements AsyncClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisClusterClient.class);

    // 用于描述cluster nodes的index的bit位数
    private static final Integer NodeIndexBitLength = 10;

    // 用于描述real cursor的bit位数，最高位永远是0
    private static final Integer RealCursorBitLength = 63 - NodeIndexBitLength;
    // RealCursorBitLength = 10 时，ClusterNodeIndexMask的二进制为 0111111111100000 0000000000000000 0000000000000000 0000000000000000
    private static final Long ClusterNodeIndexMask = ((long) Math.pow(2, NodeIndexBitLength) - 1) << RealCursorBitLength;
    // RealCursorBitLength = 10 时，RealCursorMask的二进制为 0000000000011111 1111111111111111 1111111111111111 1111111111111111
    private static final Long RealCursorMask = (long) Math.pow(2, RealCursorBitLength) - 1;


    private final RedisClusterSlotInfo clusterSlotInfo;
    private final RedisClusterResource redisClusterResource;
    private final int maxAttempts;

    public AsyncCamelliaRedisClusterClient(RedisClusterResource redisClusterResource, int maxAttempts) {
        this.redisClusterResource = redisClusterResource;
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
    }

    @Override
    public void preheat() {
        logger.info("try preheat, url = {}", redisClusterResource.getUrl());
        Set<RedisClusterSlotInfo.Node> nodes = this.clusterSlotInfo.getNodes();
        for (RedisClusterSlotInfo.Node node : nodes) {
            logger.info("try preheat, url = {}, node = {}", redisClusterResource.getUrl(), node.getAddr().getUrl());
            boolean result = RedisClientHub.preheat(node.getHost(), node.getPort(), node.getPassword());
            logger.info("preheat result = {}, url = {}, node = {}", result, redisClusterResource.getUrl(), node.getAddr().getUrl());
        }
        logger.info("preheat ok, url = {}", redisClusterResource.getUrl());
    }

    public void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> futureList) {
        if (commands.isEmpty()) return;
        if (commands.size() == 1) {
            Command command = commands.get(0);
            if (isPassThroughCommand(command)) {
                byte[][] args = command.getObjects();
                byte[] key = args[1];
                int slot = RedisClusterCRC16Utils.getSlot(key);
                RedisClient client = getClient(slot);
                if (client != null) {
                    client.sendCommand(commands, Collections.singletonList(new CompletableFutureWrapper(this, futureList.get(0), command)));
                    if (logger.isDebugEnabled()) {
                        logger.debug("sendCommand, command = {}, key = {}, slot = {}", command.getName(), Utils.bytesToString(key), slot);
                    }
                    return;
                }
            }
        }

        CommandFlusher commandFlusher = new CommandFlusher(commands.size());
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = futureList.get(i);
            RedisCommand redisCommand = command.getRedisCommand();

            RedisClient bindClient = command.getChannelInfo().getBindClient();
            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_1) {
                if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
                    boolean first = false;
                    if (bindClient == null) {
                        int randomSlot = ThreadLocalRandom.current().nextInt(RedisClusterSlotInfo.SLOT_SIZE);
                        RedisClusterSlotInfo.Node node = clusterSlotInfo.getNode(randomSlot);
                        if (node == null) {
                            future.complete(ErrorReply.NOT_AVAILABLE);
                            continue;
                        }
                        bindClient = RedisClientHub.newClient(node.getAddr());
                        command.getChannelInfo().setBindClient(bindClient);
                        first = true;
                    }
                    if (bindClient != null) {
                        AsyncTaskQueue asyncTaskQueue = command.getChannelInfo().getAsyncTaskQueue();
                        commandFlusher.flush();
                        commandFlusher.clear();
                        PubSubUtils.sendByBindClient(bindClient, asyncTaskQueue, command, future, first);
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
                    continue;
                }
                if (bindClient != null && (redisCommand == RedisCommand.UNSUBSCRIBE || redisCommand == RedisCommand.PUNSUBSCRIBE)) {
                    byte[][] objects = command.getObjects();
                    if (objects != null && objects.length > 1) {
                        for (int j = 1; j < objects.length; j++) {
                            byte[] channel = objects[j];
                            if (redisCommand == RedisCommand.UNSUBSCRIBE) {
                                command.getChannelInfo().removeSubscribeChannels(channel);
                            } else {
                                command.getChannelInfo().removePSubscribeChannels(channel);
                            }
                            if (!command.getChannelInfo().hasSubscribeChannels()) {
                                command.getChannelInfo().setBindClient(null);
                                bindClient.startIdleCheck();
                            }
                        }
                    }
                }
                if (redisCommand == RedisCommand.SCAN) {
                    scan(commandFlusher, command, future);
                    continue;
                }
            }

            if (bindClient != null) {
                commandFlusher.flush();
                commandFlusher.clear();
                PubSubUtils.sendByBindClient(bindClient, command.getChannelInfo().getAsyncTaskQueue(), command, future, false);
                continue;
            }

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.RESTRICTIVE_SUPPORT) {
                switch (redisCommand) {
                    case EVAL:
                    case EVALSHA:
                        evalOrEvalSha(command, commandFlusher, future);
                        break;
                    case PFCOUNT:
                    case SDIFF:
                    case SINTER:
                    case SUNION:
                    case PFMERGE:
                    case SINTERSTORE:
                    case SUNIONSTORE:
                    case SDIFFSTORE:
                    case RPOPLPUSH:
                        checkSlotCommandsAndSend(command, commandFlusher, future, 1, command.getObjects().length - 1);
                        break;
                    case RENAME:
                    case RENAMENX:
                    case SMOVE:
                    case LMOVE:
                    case GEOSEARCHSTORE:
                    case ZRANGESTORE:
                        checkSlotCommandsAndSend(command, commandFlusher, future, 1, 2);
                        break;
                    case ZINTERSTORE:
                    case ZUNIONSTORE:
                    case ZDIFFSTORE:
                        int keyCount = (int) Utils.bytesToNum(command.getObjects()[2]);
                        checkSlotCommandsAndSend(command, commandFlusher, future, 3, 3 + keyCount, command.getObjects()[1]);
                        break;
                    case ZDIFF:
                    case ZUNION:
                    case ZINTER:
                        int keyCount1 = (int) Utils.bytesToNum(command.getObjects()[1]);
                        checkSlotCommandsAndSend(command, commandFlusher, future, 2, 1 + keyCount1);
                        break;
                    case BITOP:
                        checkSlotCommandsAndSend(command, commandFlusher, future, 2, command.getObjects().length - 1);
                        break;
                    case MSETNX:
                        msetnx(command, commandFlusher, future);
                        break;
                    case BLPOP:
                    case BRPOP:
                    case BRPOPLPUSH:
                    case BZPOPMAX:
                    case BZPOPMIN:
                        int slot = checkSlot(command, 1, command.getObjects().length - 2);
                        blockingCommand(slot, command, commandFlusher, future);
                        break;
                    case BLMOVE:
                        int slot1 = checkSlot(command, 1, 2);
                        blockingCommand(slot1, command, commandFlusher, future);
                        break;
                    case XREAD:
                    case XREADGROUP:
                        xreadOrXreadgroup(command, commandFlusher, future);
                        break;
                    default:
                        future.complete(ErrorReply.NOT_SUPPORT);
                        break;
                }
                continue;
            }

            if (command.getRedisCommand().getCommandKeyType() != RedisCommand.CommandKeyType.SIMPLE_SINGLE) {
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
                    case XINFO:
                    case XGROUP:
                        xinfoOrXgroup(command, commandFlusher, future);
                        continueOk = true;
                        break;
                }
                if (continueOk) continue;
            }

            byte[][] args = command.getObjects();
            byte[] key = args[1];
            int slot = RedisClusterCRC16Utils.getSlot(key);

            RedisClient client = getClient(slot);
            if (logger.isDebugEnabled()) {
                logger.debug("sendCommand, command = {}, key = {}, slot = {}", command.getName(), Utils.bytesToString(key), slot);
            }
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
            commandFlusher.sendCommand(client, command, futureWrapper);
        }
        commandFlusher.flush();
    }

    private void scan(CommandFlusher commandFlusher, Command command, CompletableFuture<Reply> future) {
        byte[][] objects = command.getObjects();
        if (objects == null || objects.length <= 1) {
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            return;
        }

        long requestCursor = Utils.bytesToNum(objects[1]);
        long nodeIndex;
        long realCursor;
        if (requestCursor == 0) {
            nodeIndex = 0;
            realCursor = 0;
        } else {
            nodeIndex = (requestCursor & ClusterNodeIndexMask) >> RealCursorBitLength;
            realCursor = requestCursor & RealCursorMask;
        }

        RedisClient redisClient = getClientByIndex((int) nodeIndex);
        if (redisClient == null) {
            logger.warn("cannot find redis client for cluster index: {}, client requestCursor: {}",
                    nodeIndex, requestCursor);
            future.complete(ErrorReply.NOT_AVAILABLE);
            return;
        }

        // rewrite real requestCursor for cluster node.
        objects[1] = Utils.stringToBytes(String.valueOf(realCursor));

        final long currentNodeIndex = nodeIndex;

        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        completableFuture.thenApply((reply) -> {
            if (reply instanceof MultiBulkReply) {
                MultiBulkReply multiBulkReply = (MultiBulkReply) reply;
                long newNodeIndex;
                long newCursor;
                if (multiBulkReply.getReplies().length == 2) {
                    BulkReply cursorReply = (BulkReply) multiBulkReply.getReplies()[0];
                    long replyCursor = Utils.bytesToNum(cursorReply.getRaw());
                    if (replyCursor == 0L) {
                        if (currentNodeIndex < (getNodesSize() - 1)) {
                            newNodeIndex = currentNodeIndex + 1;
                        } else {
                            newNodeIndex = 0L;
                        }
                        newCursor = 0L;
                    } else {
                        newCursor = replyCursor;
                        newNodeIndex = currentNodeIndex;
                    }

                    if (newCursor > RealCursorMask) {
                        return new ErrorReply(String.format("Redis requestCursor is larger than %d is not supported for cluster mode.", RealCursorMask));
                    }

                    multiBulkReply.getReplies()[0] = new BulkReply(Utils.stringToBytes(String.valueOf(newNodeIndex << RealCursorBitLength | newCursor)));
                }
            }
            return reply;
        }).thenAccept(future::complete);
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, completableFuture, command);
        commandFlusher.sendCommand(redisClient, command, futureWrapper);
    }

    private RedisClient getClient(int slot) {
        RedisClient client = null;
        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts++;
            client = clusterSlotInfo.getClient(slot);
            if (client != null && client.isValid()) {
                break;
            } else {
                clusterSlotInfo.renew();
            }
        }
        return client;
    }

    private RedisClient getClientByIndex(int index) {
        return clusterSlotInfo.getClientByIndex(index);
    }

    private Integer getNodesSize() {
        return clusterSlotInfo.getNodesSize();
    }

    private static class CompletableFutureWrapper extends CompletableFuture<Reply> {
        private static final Command ASKING = new Command(new byte[][]{RedisCommand.ASKING.raw()});
        private final AsyncCamelliaRedisClusterClient clusterClient;
        private final CompletableFuture<Reply> future;
        private final Command command;
        private int attempts = 0;

        CompletableFutureWrapper(AsyncCamelliaRedisClusterClient clusterClient, CompletableFuture<Reply> future, Command command) {
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
                            ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class, log);
                            clusterClient.clusterSlotInfo.renew();
                            String[] strings = parseTargetHostAndSlot(error);
                            RedisClientAddr addr = new RedisClientAddr(strings[1], Integer.parseInt(strings[2]), clusterClient.redisClusterResource.getPassword());
                            if (command.isBlocking()) {
                                RedisClient redisClient = command.getChannelInfo().tryGetExistsRedisClientForBlockingCommand(addr);
                                if (redisClient != null && redisClient.isValid()) {
                                    ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                            "MOVED, [BlockingCommand] [RedisClient tryGet success], command = " + command.getName() + ", attempts = " + attempts);
                                    redisClient.sendCommand(Collections.singletonList(command), Collections.singletonList(this));
                                    redisClient.startIdleCheck();
                                    command.getChannelInfo().addRedisClientForBlockingCommand(redisClient);
                                } else {
                                    CompletableFuture<RedisClient> future = RedisClientHub.newAsync(addr.getHost(), addr.getPort(), addr.getPassword());
                                    future.thenAccept(client -> {
                                        try {
                                            if (client == null) {
                                                ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                        "MOVED, [BlockingCommand] [RedisClient newAsync fail], command = " + command.getName() + ", attempts = " + attempts);
                                                clusterClient.clusterSlotInfo.renew();
                                                CompletableFutureWrapper.this.future.complete(reply);
                                            } else {
                                                ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                        "MOVED, [BlockingCommand] [RedisClient newAsync success], command = " + command.getName() + ", attempts = " + attempts);
                                                client.sendCommand(Collections.singletonList(command), Collections.singletonList(CompletableFutureWrapper.this));
                                                client.startIdleCheck();
                                                command.getChannelInfo().addRedisClientForBlockingCommand(client);
                                            }
                                        } catch (Exception e) {
                                            ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                    "MOVED, [BlockingCommand] [RedisClient newAsync error], command = " + command.getName() + ", attempts = " + attempts, e);
                                            CompletableFutureWrapper.this.future.complete(reply);
                                        }
                                    });
                                }
                            } else {
                                RedisClient redisClient = RedisClientHub.tryGet(addr.getHost(), addr.getPort(), addr.getPassword());
                                if (redisClient != null) {
                                    ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                            "MOVED, [RedisClient tryGet success], command = " + command.getName() + ", attempts = " + attempts);
                                    redisClient.sendCommand(Collections.singletonList(command), Collections.singletonList(this));
                                } else {
                                    CompletableFuture<RedisClient> future = RedisClientHub.getAsync(addr.getHost(), addr.getPort(), addr.getPassword());
                                    future.thenAccept(client -> {
                                        try {
                                            if (client == null) {
                                                ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                        "MOVED, [RedisClient getAsync fail], command = " + command.getName() + ", attempts = " + attempts);
                                                clusterClient.clusterSlotInfo.renew();
                                                CompletableFutureWrapper.this.future.complete(reply);
                                            } else {
                                                ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                        "MOVED, [RedisClient getAsync success], command = " + command.getName() + ", attempts = " + attempts);
                                                client.sendCommand(Collections.singletonList(command), Collections.singletonList(CompletableFutureWrapper.this));
                                            }
                                        } catch (Exception e) {
                                            ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                    "MOVED, [RedisClient getAsync error], command = " + command.getName() + ", attempts = " + attempts, e);
                                            CompletableFutureWrapper.this.future.complete(reply);
                                        }
                                    });
                                }
                            }
                            return true;
                        } else if (error.startsWith("ASK")) {
                            attempts++;
                            String log = "ASK, command = " + command.getName() + ", attempts = " + attempts;
                            ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class, log);
                            String[] strings = parseTargetHostAndSlot(error);
                            RedisClientAddr addr = new RedisClientAddr(strings[1], Integer.parseInt(strings[2]), clusterClient.redisClusterResource.getPassword());
                            if (command.isBlocking()) {
                                RedisClient redisClient = command.getChannelInfo().tryGetExistsRedisClientForBlockingCommand(addr);
                                if (redisClient != null && redisClient.isValid()) {
                                    ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                            "ASK, [BlockingCommand] [RedisClient tryGet success], command = " + command.getName() + ", attempts = " + attempts);
                                    redisClient.sendCommand(Arrays.asList(ASKING, command), Arrays.asList(new CompletableFuture<>(), this));
                                    redisClient.startIdleCheck();
                                    command.getChannelInfo().addRedisClientForBlockingCommand(redisClient);
                                } else {
                                    CompletableFuture<RedisClient> future = RedisClientHub.newAsync(addr.getHost(), addr.getPort(), addr.getPassword());
                                    future.thenAccept(client -> {
                                        try {
                                            if (client == null) {
                                                ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                        "ASK, [BlockingCommand] [RedisClient newAsync fail], command = " + command.getName() + ", attempts = " + attempts);
                                                clusterClient.clusterSlotInfo.renew();
                                                CompletableFutureWrapper.this.future.complete(reply);
                                            } else {
                                                ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                        "ASK, [BlockingCommand] [RedisClient newAsync success], command = " + command.getName() + ", attempts = " + attempts);
                                                client.sendCommand(Arrays.asList(ASKING, command), Arrays.asList(new CompletableFuture<>(), CompletableFutureWrapper.this));
                                                client.startIdleCheck();
                                                command.getChannelInfo().addRedisClientForBlockingCommand(client);
                                            }
                                        } catch (Exception e) {
                                            ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                    "ASK, [BlockingCommand] [RedisClient newAsync error], command = " + command.getName() + ", attempts = " + attempts, e);
                                            CompletableFutureWrapper.this.future.complete(reply);
                                        }
                                    });
                                }
                            } else {
                                RedisClient redisClient = RedisClientHub.tryGet(strings[1], Integer.parseInt(strings[2]), clusterClient.redisClusterResource.getPassword());
                                if (redisClient != null) {
                                    ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                            "ASK, [RedisClient tryGet success], command = " + command.getName() + ", attempts = " + attempts);
                                    redisClient.sendCommand(Arrays.asList(ASKING, command), Arrays.asList(new CompletableFuture<>(), this));
                                } else {
                                    CompletableFuture<RedisClient> future = RedisClientHub.getAsync(strings[1], Integer.parseInt(strings[2]), clusterClient.redisClusterResource.getPassword());
                                    future.thenAccept(client -> {
                                        try {
                                            if (client == null) {
                                                ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                        "ASK, [RedisClient getAsync fail], command = " + command.getName() + ", attempts = " + attempts);
                                                clusterClient.clusterSlotInfo.renew();
                                                CompletableFutureWrapper.this.future.complete(reply);
                                            } else {
                                                ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                        "ASK, [RedisClient getAsync success], command = " + command.getName() + ", attempts = " + attempts);
                                                client.sendCommand(Arrays.asList(ASKING, command), Arrays.asList(new CompletableFuture<>(), CompletableFutureWrapper.this));
                                            }
                                        } catch (Exception e) {
                                            ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class,
                                                    "ASK, [RedisClient getAsync error], command = " + command.getName() + ", attempts = " + attempts, e);
                                            CompletableFutureWrapper.this.future.complete(reply);
                                        }
                                    });
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
        RedisClient bindClient = command.getChannelInfo().getBindClient();
        if (bindClient != null) return false;
        RedisCommand redisCommand = command.getRedisCommand();
        RedisCommand.CommandSupportType supportType = redisCommand.getSupportType();
        if (supportType == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_1
                || supportType == RedisCommand.CommandSupportType.RESTRICTIVE_SUPPORT) {
            return false;
        }
        RedisCommand.CommandKeyType commandKeyType = redisCommand.getCommandKeyType();
        return commandKeyType == RedisCommand.CommandKeyType.SIMPLE_SINGLE && !command.isBlocking();
    }

    private void msetnx(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] objects = command.getObjects();
        int slot = -1;
        for (int i = 1; i < objects.length; i += 2) {
            byte[] key = objects[i];
            int nextSlot = RedisClusterCRC16Utils.getSlot(key);
            if (slot > 0) {
                if (slot != nextSlot) {
                    future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot"));
                    return;
                }
            }
            slot = nextSlot;
        }
        RedisClient client = getClient(slot);
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
        commandFlusher.sendCommand(client, command, futureWrapper);
    }

    private void checkSlotCommandsAndSend(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future, int start, int end, byte[]... otherKeys) {
        int slot = checkSlot(command, start, end, otherKeys);
        if (slot < 0) {
            future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot"));
            return;
        }
        RedisClient client = getClient(slot);
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
        commandFlusher.sendCommand(client, command, futureWrapper);
    }

    private int checkSlot(Command command, int start, int end, byte[]... otherKeys) {
        byte[][] objects = command.getObjects();
        int slot = -1;
        for (int i = start; i <= end; i++) {
            byte[] key = objects[i];
            int nextSlot = RedisClusterCRC16Utils.getSlot(key);
            if (slot >= 0) {
                if (slot != nextSlot) {
                    return -1;
                }
            }
            slot = nextSlot;
        }
        if (otherKeys != null) {
            for (byte[] key : otherKeys) {
                int nextSlot = RedisClusterCRC16Utils.getSlot(key);
                if (slot >= 0) {
                    if (slot != nextSlot) {
                        return -1;
                    }
                }
                slot = nextSlot;
            }
        }
        return slot;
    }

    private void evalOrEvalSha(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] objects = command.getObjects();
        long keyCount = Utils.bytesToNum(objects[2]);
        if (keyCount == 0) {
            RedisClient client = getClient(0);
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
            commandFlusher.sendCommand(client, command, futureWrapper);
        } else if (keyCount == 1) {
            byte[] key = objects[3];
            int slot = RedisClusterCRC16Utils.getSlot(key);
            RedisClient client = getClient(slot);
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
            commandFlusher.sendCommand(client, command, futureWrapper);
        } else {
            byte[] key = objects[3];
            int slot = RedisClusterCRC16Utils.getSlot(key);
            for (int i = 4; i < 3 + keyCount; i++) {
                int nextSlot = RedisClusterCRC16Utils.getSlot(objects[i]);
                if (slot != nextSlot) {
                    future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot"));
                    return;
                }
            }
            RedisClient client = getClient(slot);
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
            commandFlusher.sendCommand(client, command, futureWrapper);
        }
    }

    private void mget(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] args = command.getObjects();
        List<CompletableFuture<Reply>> futureList = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            byte[] key = args[i];
            int slot = RedisClusterCRC16Utils.getSlot(key);
            RedisClient client = getClient(slot);
            Command subCommand = new Command(new byte[][]{RedisCommand.GET.raw(), key});

            CompletableFuture<Reply> subFuture = new CompletableFuture<>();
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, subFuture, subCommand);
            commandFlusher.sendCommand(client, subCommand, futureWrapper);
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
        AsyncUtils.allOf(futureList).thenAccept(replies -> {
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
        List<CompletableFuture<Reply>> futureList = new ArrayList<>();
        for (int i = 1; i < args.length; i++, i++) {
            byte[] key = args[i];
            byte[] value = args[i + 1];
            int slot = RedisClusterCRC16Utils.getSlot(key);
            RedisClient client = getClient(slot);
            Command subCommand = new Command(new byte[][]{RedisCommand.SET.raw(), key, value});

            CompletableFuture<Reply> subFuture = new CompletableFuture<>();
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, subFuture, subCommand);
            commandFlusher.sendCommand(client, subCommand, futureWrapper);
            futureList.add(subFuture);
        }
        if (futureList.size() == 1) {
            CompletableFuture<Reply> completableFuture = futureList.get(0);
            completableFuture.thenAccept(future::complete);
            return;
        }
        AsyncUtils.allOf(futureList).thenAccept(replies -> future.complete(Utils.mergeStatusReply(replies)));
    }

    private void simpleIntegerReplyMerge(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] args = command.getObjects();
        List<CompletableFuture<Reply>> futureList = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            byte[] key = args[i];
            int slot = RedisClusterCRC16Utils.getSlot(key);
            RedisClient client = getClient(slot);
            Command subCommand = new Command(new byte[][]{args[0], key});

            CompletableFuture<Reply> subFuture = new CompletableFuture<>();
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, subFuture, subCommand);
            commandFlusher.sendCommand(client, subCommand, futureWrapper);
            futureList.add(subFuture);
        }
        if (futureList.size() == 1) {
            CompletableFuture<Reply> completableFuture = futureList.get(0);
            completableFuture.thenAccept(future::complete);
            return;
        }
        AsyncUtils.allOf(futureList).thenAccept(replies -> future.complete(Utils.mergeIntegerReply(replies)));
    }

    private void blockingCommand(int slot, Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        if (slot < 0) {
            future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot"));
            return;
        }
        RedisClusterSlotInfo.Node node = clusterSlotInfo.getNode(slot);
        if (node == null) {
            ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class, "blockingCommand getNode, slot=" + slot + " fail");
            future.complete(ErrorReply.NOT_AVAILABLE);
            return;
        }
        RedisClient client = command.getChannelInfo().tryGetExistsRedisClientForBlockingCommand(node.getAddr());
        if (client == null || !client.isValid()) {
            client = RedisClientHub.newClient(node.getAddr());
            if (client == null) {
                ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class, "blockingCommand newClient, node=" + node.getAddr() + " fail");
                future.complete(ErrorReply.NOT_AVAILABLE);
                return;
            }
        }
        commandFlusher.flush();
        commandFlusher.clear();
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
        client.sendCommand(Collections.singletonList(command), Collections.singletonList(futureWrapper));
        client.startIdleCheck();
        command.getChannelInfo().addRedisClientForBlockingCommand(client);
    }

    private void xreadOrXreadgroup(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] objects = command.getObjects();
        int index = -1;
        for (int i = 1; i < objects.length; i++) {
            String string = new String(objects[i], Utils.utf8Charset);
            if (string.equalsIgnoreCase(RedisKeyword.STREAMS.name())) {
                index = i;
                break;
            }
        }
        int last = objects.length - index - 1;
        int keyCount = last / 2;
        int slot = checkSlot(command, index + 1, index + keyCount);
        if (command.isBlocking()) {
            blockingCommand(slot, command, commandFlusher, future);
        } else {
            if (slot < 0) {
                future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot"));
                return;
            }
            RedisClient client = getClient(slot);
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
            commandFlusher.sendCommand(client, command, futureWrapper);
        }
    }

    private void xinfoOrXgroup(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[] key = command.getObjects()[2];
        int slot = RedisClusterCRC16Utils.getSlot(key);
        RedisClient client = getClient(slot);
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
        commandFlusher.sendCommand(client, command, futureWrapper);
    }
}
