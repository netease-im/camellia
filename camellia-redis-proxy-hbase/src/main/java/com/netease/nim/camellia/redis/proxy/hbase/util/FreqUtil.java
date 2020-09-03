package com.netease.nim.camellia.redis.proxy.hbase.util;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


/**
 *
 * Created by caojiajun on 2020/6/28.
 */
public class FreqUtil {

    private static final Logger logger = LoggerFactory.getLogger(FreqUtil.class);

    public static boolean freq(CamelliaRedisTemplate redisTemplate, byte[] key, int threshold, long expireMillis) {
        if (!RedisHBaseConfiguration.freqEnable()) {
            return true;
        }
        try {
            Long incr = redisTemplate.incr(key);
            if (incr == 1) {
                redisTemplate.pexpire(key, expireMillis);
            }
            if (incr > threshold && incr % (threshold * 10) == 0) {
                Long ttl = redisTemplate.ttl(key);
                if (ttl == -1) {
                    redisTemplate.pexpire(key, expireMillis);
                }
            }
            return incr <= threshold;
        } catch (Exception e) {
            logger.error("freq error, key = {}, threshold = {}, expireMillis = {}, default.pass = {}, ex = {}",
                    SafeEncoder.encode(key), threshold, expireMillis, RedisHBaseConfiguration.freqDefaultPass(), e.toString());
            return RedisHBaseConfiguration.freqDefaultPass();
        }
    }

    private static final Counter counter = new Counter();

    public static boolean hbaseGetStandaloneFreq() {
        try {
            if (!RedisHBaseConfiguration.freqEnable()) {
                return true;
            }
            counter.freshIfNeed(RedisHBaseConfiguration.hbaseGetStandaloneFreqMillis());
            long current = counter.incrementAndGet();
            return current <= RedisHBaseConfiguration.hbaseGetStandaloneFreqThreshold();
        } catch (Exception e) {
            logger.error("hbaseGetStandaloneFreq error, threshold = {}, expireMillis = {}, default.pass = {}, ex = {}",
                    RedisHBaseConfiguration.hbaseGetStandaloneFreqThreshold(), RedisHBaseConfiguration.hbaseGetStandaloneFreqMillis(),
                    RedisHBaseConfiguration.freqDefaultPass(), e.toString());
            return RedisHBaseConfiguration.freqDefaultPass();
        }
    }

    private static volatile long currentTimeMillis = System.currentTimeMillis();
    static {
        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                currentTimeMillis = System.currentTimeMillis();
            }
        }, "timestamp-cache").start();
    }

    private static class Counter {
        private volatile long lastTimestamp = currentTimeMillis;
        private final AtomicLong count = new AtomicLong();
        private final AtomicBoolean lock = new AtomicBoolean(false);

        public void freshIfNeed(long expireMillis) {
            if (currentTimeMillis - lastTimestamp >= expireMillis) {
                if (lock.compareAndSet(false, true)) {
                    try {
                        if (currentTimeMillis - lastTimestamp >= expireMillis) {
                            count.set(0);
                            lastTimestamp = currentTimeMillis;
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
