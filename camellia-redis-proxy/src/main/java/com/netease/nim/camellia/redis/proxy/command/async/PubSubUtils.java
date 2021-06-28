package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.Command;
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

    public static void sendByBindClient(RedisClient client, AsyncTaskQueue asyncTaskQueue,
                                        Command command, CompletableFuture<Reply> future, boolean first) {
        List<CompletableFuture<Reply>> futures = new ArrayList<>();
        if (future != null) {
            CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
            futures.add(completableFuture);
            completableFuture.thenAccept(reply -> {
                if (first) {
                    future.complete(reply);
                } else {
                    asyncTaskQueue.reply(reply);
                }
                checkSubscribeReply(reply, asyncTaskQueue);
            });
        }
        if (client.queueSize() < 8) {
            for (int j = 0; j < 16; j++) {
                CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                completableFuture.thenAccept(reply -> {
                    if (client.queueSize() < 8 && client.isValid()) {
                        sendByBindClient(client, asyncTaskQueue, null, null, false);
                    }
                    asyncTaskQueue.reply(reply);
                    checkSubscribeReply(reply, asyncTaskQueue);
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

    public static void checkSubscribeReply(Reply reply, AsyncTaskQueue asyncTaskQueue) {
        try {
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies.length > 0) {
                    Reply firstReply = replies[0];
                    if (firstReply instanceof BulkReply) {
                        byte[] raw = ((BulkReply) firstReply).getRaw();
                        String str = Utils.bytesToString(raw);
                        if (str.equalsIgnoreCase(RedisCommand.SUBSCRIBE.name())) {
                            checkSubscribe(replies, asyncTaskQueue);
                        } else if (str.equalsIgnoreCase(RedisCommand.UNSUBSCRIBE.name())) {
                            checkSubscribe(replies, asyncTaskQueue);
                        } else if (str.equalsIgnoreCase(RedisCommand.PSUBSCRIBE.name())) {
                            checkSubscribe(replies, asyncTaskQueue);
                        } else if (str.equalsIgnoreCase(RedisCommand.PUNSUBSCRIBE.name())) {
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
