package com.netease.nim.camellia.redis.proxy.upstream.utils;


import com.netease.nim.camellia.redis.proxy.conf.MultiWriteMode;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class CompletableFutureUtils {

    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futuresList) {
        CompletableFuture<T>[] futures = new CompletableFuture[futuresList.size()];
        for (int i=0; i<futuresList.size(); i++) {
            futures[i] = futuresList.get(i);
        }
        CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(futures);
        return allFuturesResult.thenApply(v ->
                futuresList.stream().
                        map(CompletableFuture::join).
                        collect(Collectors.toList())
        );
    }

    public static CompletableFuture<Reply> finalReply(List<CompletableFuture<Reply>> futureList, MultiWriteMode multiWriteMode) {
        if (multiWriteMode == MultiWriteMode.FIRST_RESOURCE_ONLY) {
            return futureList.get(0);
        }
        if (futureList.size() == 1) {
            return futureList.get(0);
        }
        final CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        CompletableFuture<List<Reply>> listCompletableFuture = allOf(futureList);
        listCompletableFuture.thenAccept(replies -> {
            if (multiWriteMode == MultiWriteMode.ALL_RESOURCES_NO_CHECK) {
                completableFuture.complete(replies.get(0));
            } else if (multiWriteMode == MultiWriteMode.ALL_RESOURCES_CHECK_ERROR) {
                for (Reply reply : replies) {
                    if (reply instanceof ErrorReply) {
                        completableFuture.complete(reply);
                        return;
                    }
                }
                completableFuture.complete(replies.get(0));
            }
        });
        return completableFuture;
    }
}
