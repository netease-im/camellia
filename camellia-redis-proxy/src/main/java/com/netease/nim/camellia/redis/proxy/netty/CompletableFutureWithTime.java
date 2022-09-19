package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.monitor.UpstreamRedisSpendTimeMonitor;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureWithTime<T> extends CompletableFuture<T> {

    private final RedisClientAddr addr;
    private final CompletableFuture<T> future;
    private final long startTime;

    public CompletableFutureWithTime(CompletableFuture<T> future, RedisClientAddr addr, long startTime) {
        this.future = future;
        this.addr = addr;
        this.startTime = startTime;
    }

    @Override
    public boolean complete(T value) {
        UpstreamRedisSpendTimeMonitor.incr(addr, System.nanoTime() - startTime);
        return future.complete(value);
    }
}
