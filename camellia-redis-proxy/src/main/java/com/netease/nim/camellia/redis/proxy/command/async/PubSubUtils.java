package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2020/9/29
 */
public class PubSubUtils {

    public static void sendByBindClient(RedisClient client, AsyncTaskQueue asyncTaskQueue, Command command, CompletableFuture<Reply> future) {
        List<CompletableFuture<Reply>> futures = new ArrayList<>();
        if (future != null) {
            futures.add(future);
        }
        for (int j=0; j<16; j++) {
            AsyncTask task = new AsyncTask(asyncTaskQueue, command, null, null);
            asyncTaskQueue.add(task);
            CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
            completableFuture.thenAccept(reply -> {
                if (asyncTaskQueue.size() < 8) {
                    sendByBindClient(client, asyncTaskQueue, null, null);
                }
                task.replyCompleted(reply);
            });
            futures.add(completableFuture);
        }
        if (command != null) {
            client.sendCommand(Collections.singletonList(command), futures);
        } else {
            client.sendCommand(Collections.emptyList(), futures);
        }
    }
}
