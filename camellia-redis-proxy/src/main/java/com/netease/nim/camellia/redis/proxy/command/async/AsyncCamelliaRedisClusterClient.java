package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.redis.resource.RedisClusterResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.JedisClusterCRC16;
import redis.clients.util.SafeEncoder;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class AsyncCamelliaRedisClusterClient implements AsyncClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisClusterClient.class);

    private RedisClusterSlotInfo clusterSlotInfo;
    private RedisClusterResource redisClusterResource;
    private int maxAttempts = 5;

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

            if (command.getName().equalsIgnoreCase(RedisCommand.EXISTS.name())) {
                if (command.getObjects().length > 2) {
                    simpleIntegerReplyMerge(command, commandFlusher, future);
                    continue;
                }
            } else if (command.getName().equalsIgnoreCase(RedisCommand.DEL.name())) {
                if (command.getObjects().length > 2) {
                    simpleIntegerReplyMerge(command, commandFlusher, future);
                    continue;
                }
            } else if (command.getName().equalsIgnoreCase(RedisCommand.MSET.name())) {
                if (command.getObjects().length > 3) {
                    mset(command, commandFlusher, future);
                    continue;
                }
            } else if (command.getName().equalsIgnoreCase(RedisCommand.MGET.name())) {
                if (command.getObjects().length > 2) {
                    mget(command, commandFlusher, future);
                    continue;
                }
            }

            byte[][] args = command.getObjects();
            byte[] key = args[1];
            int slot = JedisClusterCRC16.getSlot(key);

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
        private AsyncCamelliaRedisClusterClient clusterClient;
        private CompletableFuture<Reply> future;
        private Command command;
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

        private static String[] extractParts(String from){
            int idx     = from.lastIndexOf(":");
            String host = idx != -1 ? from.substring(0, idx)  : from;
            String port = idx != -1 ? from.substring(idx + 1) : "";
            return new String[] { host, port };
        }
    }

    private void mget(Command command, CommandFlusher commandFlusher, CompletableFuture<Reply> future) {
        byte[][] args = command.getObjects();
        List<CompletableFuture<Reply>> futureList = new ArrayList<>();
        for (int i=1; i<args.length; i++) {
            byte[] key = args[i];
            int slot = JedisClusterCRC16.getSlot(key);
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
            int slot = JedisClusterCRC16.getSlot(key);
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
            int slot = JedisClusterCRC16.getSlot(key);
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
}
