package com.netease.nim.camellia.redis.proxy.command.async;


import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncUtils {

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
}
