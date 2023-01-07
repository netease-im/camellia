package com.netease.nim.camellia.redis.proxy.upstream.utils;

import com.netease.nim.camellia.redis.proxy.command.AsyncTaskQueue;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.plugin.converter.KeyConverter;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2020/9/29
 */
public class PubSubUtils {

    public static void sendByBindClient(RedisClient client, AsyncTaskQueue asyncTaskQueue,
                                        Command command, CompletableFuture<Reply> future, boolean first) {
        sendByBindClient(client, asyncTaskQueue, command, future, first, command.getRedisCommand());
    }

    private static void sendByBindClient(RedisClient client, AsyncTaskQueue asyncTaskQueue,
                                        Command command, CompletableFuture<Reply> future, boolean first, RedisCommand redisCommand) {
        List<CompletableFuture<Reply>> futures = new ArrayList<>();
        if (future != null) {
            CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
            futures.add(completableFuture);
            completableFuture.thenAccept(reply -> {
                checkSubscribeReply(reply, asyncTaskQueue);
                if (first) {
                    future.complete(reply);
                } else {
                    asyncTaskQueue.reply(redisCommand, reply);
                }
            });
        }
        if (client.queueSize() < 8) {
            for (int j = 0; j < 16; j++) {
                CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                completableFuture.thenAccept(reply -> {
                    checkSubscribeReply(reply, asyncTaskQueue);
                    if (client.queueSize() < 8 && client.isValid()) {
                        sendByBindClient(client, asyncTaskQueue, null, null, false, redisCommand);
                    }
                    asyncTaskQueue.reply(redisCommand, reply);
                });
                futures.add(completableFuture);
            }
        }
        if (command != null) {
            client.sendCommand(Collections.singletonList(command), futures);
        } else {
            client.sendCommand(Collections.emptyList(), futures);
        }
    }

    public static void checkKeyConverter(RedisCommand redisCommand, CommandContext commandContext, KeyConverter keyConverter, Reply reply) {
        if (keyConverter != null) {
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies.length > 1) {
                    if (replies[0] instanceof BulkReply) {
                        String type = Utils.bytesToString(((BulkReply) replies[0]).getRaw());
                        if (type.equalsIgnoreCase("psubscribe") || type.equalsIgnoreCase("subscribe")
                                || type.equalsIgnoreCase("unsubscribe") || type.equalsIgnoreCase("punsubscribe")
                                || type.equalsIgnoreCase("message")) {
                            if (replies.length == 3) {
                                Reply reply1 = replies[1];
                                if (reply1 instanceof BulkReply) {
                                    byte[] raw = ((BulkReply) reply1).getRaw();
                                    byte[] convert = keyConverter.reverseConvert(commandContext, redisCommand, raw);
                                    ((BulkReply) reply1).updateRaw(convert);
                                }
                            }
                        } else if (type.equalsIgnoreCase("pmessage")) {
                            if (replies.length == 4) {
                                Reply reply1 = replies[1];
                                if (reply1 instanceof BulkReply) {
                                    byte[] raw = ((BulkReply) reply1).getRaw();
                                    byte[] convert = keyConverter.reverseConvert(commandContext, redisCommand, raw);
                                    ((BulkReply) reply1).updateRaw(convert);
                                }
                                Reply reply2 = replies[2];
                                if (reply2 instanceof BulkReply) {
                                    byte[] raw = ((BulkReply) reply2).getRaw();
                                    byte[] convert = keyConverter.reverseConvert(commandContext, redisCommand, raw);
                                    ((BulkReply) reply2).updateRaw(convert);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void checkSubscribeReply(Reply reply, AsyncTaskQueue asyncTaskQueue) {
        try {
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies.length > 0) {
                    Reply firstReply = replies[0];
                    if (firstReply instanceof BulkReply) {
                        byte[] raw = ((BulkReply) firstReply).getRaw();
                        String str = Utils.bytesToString(raw);
                        if (str.equalsIgnoreCase(RedisCommand.SUBSCRIBE.strRaw())) {
                            checkSubscribe(replies, asyncTaskQueue);
                        } else if (str.equalsIgnoreCase(RedisCommand.UNSUBSCRIBE.strRaw())) {
                            checkSubscribe(replies, asyncTaskQueue);
                        } else if (str.equalsIgnoreCase(RedisCommand.PSUBSCRIBE.strRaw())) {
                            checkSubscribe(replies, asyncTaskQueue);
                        } else if (str.equalsIgnoreCase(RedisCommand.PUNSUBSCRIBE.strRaw())) {
                            checkSubscribe(replies, asyncTaskQueue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(PubSubUtils.class, "checkSubscribeReply error", e);
        }
    }

    private static void checkSubscribe(Reply[] replies, AsyncTaskQueue asyncTaskQueue) {
        if (replies != null && replies.length >= 2) {
            Reply reply = replies[2];
            if (reply instanceof IntegerReply) {
                if (((IntegerReply) reply).getInteger() <= 0) {
                    asyncTaskQueue.getChannelInfo().setInSubscribe(false);
                }
            }
        }
    }
}
