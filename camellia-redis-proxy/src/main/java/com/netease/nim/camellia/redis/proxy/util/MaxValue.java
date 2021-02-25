package com.netease.nim.camellia.redis.proxy.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2021/2/25
 */
public class MaxValue {
    private final AtomicLong max = new AtomicLong(0L);

    public MaxValue() {
        max.set(0L);
    }

    public MaxValue(long initializer) {
        max.set(initializer);
    }

    public void update(long value) {
        while (true) {
            long oldValue = max.get();
            if (value > oldValue) {
                if (max.compareAndSet(oldValue, value)) {
                    break;
                }
            } else {
                break;
            }
        }
    }

    public long getAndSet(long newValue) {
        return max.getAndSet(newValue);
    }

    public long get() {
        return max.get();
    }
}
