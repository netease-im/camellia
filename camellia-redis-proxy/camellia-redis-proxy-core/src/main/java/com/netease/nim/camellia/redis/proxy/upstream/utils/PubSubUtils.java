package com.netease.nim.camellia.redis.proxy.upstream.utils;

import com.netease.nim.camellia.redis.proxy.command.CommandTaskQueue;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.plugin.converter.KeyConverter;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2020/9/29
 */
public class PubSubUtils {

    public static void sendByBindClient(RedisConnection connection, CommandTaskQueue taskQueue,
                                        Command command, CompletableFuture<Reply> future, boolean first) {
        sendByBindClient(connection, taskQueue, command, future, first, command.getRedisCommand());
    }

    private static void sendByBindClient(RedisConnection connection, CommandTaskQueue asyncTaskQueue,
                                         Command command, CompletableFuture<Reply> future, boolean first, RedisCommand redisCommand) {
        List<CompletableFuture<Reply>> futures = new ArrayList<>();
        if (future != null) {
            CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
            futures.add(completableFuture);
            completableFuture.thenAccept(reply -> {
                //parse reply must before send reply to connection
                Long subscribeChannelCount = tryGetSubscribeChannelCount(reply);
                if (first) {
                    future.complete(reply);
                } else {
                    asyncTaskQueue.reply(redisCommand, reply);
                }
                //after send reply, update channel subscribe status
                if (subscribeChannelCount != null && subscribeChannelCount <= 0) {
                    asyncTaskQueue.getChannelInfo().setInSubscribe(false);
                }
            });
        }
        if (connection.queueSize() < 8) {
            for (int j = 0; j < 16; j++) {
                CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                completableFuture.thenAccept(reply -> {
                    if (connection.queueSize() < 8 && connection.isValid()) {
                        sendByBindClient(connection, asyncTaskQueue, null, null, false, redisCommand);
                    }
                    //parse reply must before send reply to connection
                    Long subscribeChannelCount = tryGetSubscribeChannelCount(reply);
                    asyncTaskQueue.reply(redisCommand, reply);
                    //after send reply, update channel subscribe status
                    if (subscribeChannelCount != null && subscribeChannelCount <= 0) {
                        asyncTaskQueue.getChannelInfo().setInSubscribe(false);
                    }
                });
                futures.add(completableFuture);
            }
        }
        if (command != null) {
            connection.sendCommand(Collections.singletonList(command), futures);
        } else {
            connection.sendCommand(Collections.emptyList(), futures);
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

    private static Long tryGetSubscribeChannelCount(Reply reply) {
        try {
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies.length > 0) {
                    Reply firstReply = replies[0];
                    if (firstReply instanceof BulkReply) {
                        byte[] raw = ((BulkReply) firstReply).getRaw();
                        String str = Utils.bytesToString(raw);
                        if (str.equalsIgnoreCase(RedisCommand.SUBSCRIBE.strRaw())) {
                            return subscribeChannelCount(replies);
                        } else if (str.equalsIgnoreCase(RedisCommand.UNSUBSCRIBE.strRaw())) {
                            return subscribeChannelCount(replies);
                        } else if (str.equalsIgnoreCase(RedisCommand.PSUBSCRIBE.strRaw())) {
                            return subscribeChannelCount(replies);
                        } else if (str.equalsIgnoreCase(RedisCommand.PUNSUBSCRIBE.strRaw())) {
                            return subscribeChannelCount(replies);
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            ErrorLogCollector.collect(PubSubUtils.class, "tryGetSubscribeChannelCount error", e);
            return null;
        }
    }

    private static Long subscribeChannelCount(Reply[] replies) {
        if (replies != null && replies.length >= 2) {
            Reply reply = replies[2];
            if (reply instanceof IntegerReply) {
                return ((IntegerReply) reply).getInteger();
            }
        }
        return null;
    }
}
