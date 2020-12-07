package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.redis.resource.RedisClusterResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class AsyncCamelliaRedisClusterClient implements AsyncClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisClusterClient.class);

    private final RedisClusterSlotInfo clusterSlotInfo;
    private final RedisClusterResource redisClusterResource;
    private final int maxAttempts;

    public AsyncCamelliaRedisClusterClient(RedisClusterResource redisClusterResource, int maxAttempts) {
        this.redisClusterResource = redisClusterResource;
        this.maxAttempts = maxAttempts;
        this.clusterSlotInfo = new RedisClusterSlotInfo(redisClusterResource);
        boolean renew = this.clusterSlotInfo.renew();
        if (!renew) {
            throw new CamelliaRedisException("RedisClusterSlotInfo init fail");
        }
    }

    public void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> futureList) {
        if (commands.isEmpty()) return;
        CommandFlusher commandFlusher = new CommandFlusher();
        for (int i=0; i<commands.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = futureList.get(i);
            RedisCommand redisCommand = command.getRedisCommand();

            RedisClient bindClient = command.getChannelInfo().getBindClient();
            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_1) {
                if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
                    if (bindClient == null) {
                        int randomSlot = ThreadLocalRandom.current().nextInt(RedisClusterSlotInfo.SLOT_SIZE);
                        bindClient = RedisClientHub.newClient(clusterSlotInfo.getNode(randomSlot).getAddr());
                        command.getChannelInfo().setBindClient(bindClient);
                    }
                    if (bindClient != null) {
                        AsyncTaskQueue asyncTaskQueue = command.getChannelInfo().getAsyncTaskQueue();
                        commandFlusher.flush();
                        commandFlusher.clear();
                        PubSubUtils.sendByBindClient(bindClient, asyncTaskQueue, command, future);
                    } else {
                        future.complete(ErrorReply.NOT_AVAILABLE);
                    }
                    continue;
                }
            }

            if (bindClient != null) {
                commandFlusher.flush();
                commandFlusher.clear();
                bindClient.sendCommand(Collections.singletonList(command), Collections.singletonList(future));
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
                        checkSlotCommandsAndSend(command, commandFlusher, future, 1, 2);
                        break;
                    case ZINTERSTORE:
                    case ZUNIONSTORE:
                        int keyCount = (int) Utils.bytesToNum(command.getObjects()[2]);
                        checkSlotCommandsAndSend(command, commandFlusher, future, 3, 3 + keyCount, command.getObjects()[1]);
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
                        int slot = checkSlot(command, 1, command.getObjects().length - 2);
                        blockingCommand(slot, command, commandFlusher, future);
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

            boolean continueOk = false;
            switch (redisCommand) {
                case EXISTS:
                case UNLINK:
                case TOUCH:
                case DEL: {
                    if (command.getObjects().length > 2) {
                        simpleIntegerReplyMerge(command, commandFlusher, future);
                        continueOk = true;
                    }
                    break;
                }
                case MSET: {
                    if (command.getObjects().length > 3) {
                        mset(command, commandFlusher, future);
                        continueOk = true;
                    }
                    break;
                }
                case MGET: {
                    if (command.getObjects().length > 2) {
                        mget(command, commandFlusher, future);
                        continueOk = true;
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

            byte[][] args = command.getObjects();
            byte[] key = args[1];
            int slot = RedisClusterCRC16Utils.getSlot(key);

            RedisClient client = getClient(slot);
            if (logger.isDebugEnabled()) {
                logger.debug("sendCommand, command = {}, key = {}, slot = {}", command.getName(), SafeEncoder.encode(key), slot);
            }
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
            commandFlusher.sendCommand(client, command, futureWrapper);
        }
        commandFlusher.flush();
    }

    private RedisClient getClient(int slot) {
        RedisClient client = null;
        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts ++;
            client = clusterSlotInfo.getClient(slot);
            if (client != null && client.isValid()) {
                break;
            } else {
                clusterSlotInfo.renew();
            }
        }
        return client;
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
                            RedisClient redisClient = RedisClientHub.get(strings[1], Integer.parseInt(strings[2]), clusterClient.redisClusterResource.getPassword());
                            if (redisClient != null) {
                                redisClient.sendCommand(Collections.singletonList(command), Collections.singletonList(this));
                                return true;
                            } else {
                                clusterClient.clusterSlotInfo.renew();
                            }
                        } else if (error.startsWith("ASK")) {
                            attempts++;
                            String log = "ASK, command = " + command.getName() + ", attempts = " + attempts;
                            ErrorLogCollector.collect(AsyncCamelliaRedisClusterClient.class, log);
                            String[] strings = parseTargetHostAndSlot(error);
                            RedisClient redisClient = RedisClientHub.get(strings[1], Integer.parseInt(strings[2]), clusterClient.redisClusterResource.getPassword());
                            if (redisClient != null) {
                                redisClient.sendCommand(Arrays.asList(ASKING, command), Arrays.asList(new CompletableFuture<>(), this));
                                return true;
                            } else {
                                clusterClient.clusterSlotInfo.renew();
                            }
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
            int idx     = from.lastIndexOf(":");
            String host = idx != -1 ? from.substring(0, idx)  : from;
            String port = idx != -1 ? from.substring(idx + 1) : "";
            return new String[] { host, port };
        }
    }

    private void msetnx(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] objects = command.getObjects();
        int slot = -1;
        for (int i=1; i<objects.length; i+=2) {
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

    private void checkSlotCommandsAndSend(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future, int start, int end, byte[]...otherKeys) {
        int slot = checkSlot(command, start, end, otherKeys);
        if (slot < 0) {
            future.complete(new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot"));
            return;
        }
        RedisClient client = getClient(slot);
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
        commandFlusher.sendCommand(client, command, futureWrapper);
    }

    private int checkSlot(Command command, int start, int end, byte[]...otherKeys) {
        byte[][] objects = command.getObjects();
        int slot = -1;
        for (int i=start; i<=end; i++) {
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
            for (int i=4; i<3+keyCount; i++) {
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
        for (int i=1; i<args.length; i++) {
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
            for (int i=0; i<replies.size(); i++) {
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
        for (int i=1; i<args.length; i++, i++) {
            byte[] key = args[i];
            byte[] value = args[i+1];
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
        for (int i=1; i<args.length; i++) {
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
            future.complete(ErrorReply.NOT_AVAILABLE);
            return;
        }
        RedisClient client = RedisClientHub.newClient(node.getAddr());
        if (client == null) {
            future.complete(ErrorReply.NOT_AVAILABLE);
            return;
        }
        commandFlusher.flush();
        commandFlusher.clear();
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
        client.sendCommand(Collections.singletonList(command), Collections.singletonList(futureWrapper));
        RedisClientHub.delayStopIfIdle(client);
        command.getChannelInfo().addRedisClientForBlockingCommand(client);
    }

    private void xreadOrXreadgroup(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] objects = command.getObjects();
        int index = -1;
        for (int i=1; i<objects.length; i++) {
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
            RedisClient client = clusterSlotInfo.getClient(slot);
            CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
            commandFlusher.sendCommand(client, command, futureWrapper);
        }
    }

    private void xinfoOrXgroup(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[] key = command.getObjects()[2];
        int slot = RedisClusterCRC16Utils.getSlot(key);
        RedisClient client = clusterSlotInfo.getClient(slot);
        CompletableFutureWrapper futureWrapper = new CompletableFutureWrapper(this, future, command);
        commandFlusher.sendCommand(client, command, futureWrapper);
    }
}
