package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.CountStats;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2025/2/8
 */
public class CountCollector {
    private final LongAdder sum = new LongAdder();
    private final LongAdder count = new LongAdder();
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

    public void update(long value) {
        sum.add(value);
        count.increment();
        long old = max.get();
        while (value > old) {
            if (max.compareAndSet(old, value)) {
                break;
            } else {
                old = max.get();
            }
        }
    }

    public CountStats getStats() {
        long s = sum.sumThenReset();
        long c = count.sumThenReset();
        long m = max.getAndSet(Long.MIN_VALUE);
        if (m == Long.MIN_VALUE) {
            m = 0;
        }
        CountStats countStats = new CountStats();
        countStats.setCount(c);
        if (c == 0) {
            countStats.setAvg(0);
        } else {
            countStats.setAvg(s / (c * 1.0));
        }
        countStats.setMax(m);
        return countStats;
    }
}
