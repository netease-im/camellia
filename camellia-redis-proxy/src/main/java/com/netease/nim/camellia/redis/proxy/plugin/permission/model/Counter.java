package com.netease.nim.camellia.redis.proxy.plugin.permission.model;

import com.netease.nim.camellia.redis.proxy.util.TimeCache;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author anhdt9
 * @since 22/12/2022
 */
public class Counter {
    private volatile long timestamp = TimeCache.currentMillis;
    private final LongAdder count = new LongAdder();
    private final AtomicBoolean lock = new AtomicBoolean();

    public long incrementAndGet(long expireMillis) {
        if (TimeCache.currentMillis - timestamp > expireMillis) {
            if (lock.compareAndSet(false, true)) {
                try {
                    if (TimeCache.currentMillis - timestamp > expireMillis) {
                        timestamp = TimeCache.currentMillis;
                        count.reset();
                    }
                } finally {
                    lock.compareAndSet(true, false);
                }
            }
        }
        count.increment();
        return count.sum();
    }

}
