package com.netease.nim.camellia.redis.proxy.hbase.util;

import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2021/1/26
 */
public class FreqUtils {

    private static final Logger logger = LoggerFactory.getLogger(FreqUtils.class);

    private static final Counter counterOfRead = new Counter();

    public static boolean hbaseReadFreq() {
        try {
            if (!RedisHBaseConfiguration.hbaseReadFreqEnable()) {
                return true;
            }
            counterOfRead.freshIfNeed(RedisHBaseConfiguration.hbaseReadFreqCheckMillis());
            long current = counterOfRead.incrementAndGet();
            return current <= RedisHBaseConfiguration.hbaseReadFreqCheckThreshold();
        } catch (Exception e) {
            logger.error("freq error", e);
            return true;
        }
    }

    private static class Counter {
        private volatile long lastTimestamp = TimeCache.currentMillis;
        private final AtomicLong count = new AtomicLong();
        private final AtomicBoolean lock = new AtomicBoolean(false);

        public void freshIfNeed(long expireMillis) {
            if (TimeCache.currentMillis - lastTimestamp >= expireMillis) {
                if (lock.compareAndSet(false, true)) {
                    try {
                        if (TimeCache.currentMillis - lastTimestamp >= expireMillis) {
                            count.set(0);
                            lastTimestamp = TimeCache.currentMillis;
                        }
                    } finally {
                        lock.compareAndSet(true, false);
                    }
                }
            }
        }

        public long incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
