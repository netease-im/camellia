package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 为了避免打印大量相同的Error日志导致影响性能，收集在这里定时合并打印
 * Created by caojiajun on 2020/1/10.
 */
public class ErrorLogCollector {

    private static final Logger logger = LoggerFactory.getLogger(ErrorLogCollector.class);

    private static ConcurrentHashMap<String, AtomicLong> logMap = new ConcurrentHashMap<>();
    static {
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("error-log-collector"))
                .scheduleAtFixedRate(ErrorLogCollector::print, 10, 10, TimeUnit.SECONDS);
    }

    public static void collect(Class clazz, String log) {
        AtomicLong count = logMap.computeIfAbsent(clazz.getName() + ":" + log, k -> new AtomicLong(0L));
        count.incrementAndGet();
    }

    private static void print() {
        if (logMap.isEmpty()) return;
        for (Map.Entry<String, AtomicLong> entry : logMap.entrySet()) {
            String log = entry.getKey();
            long count = entry.getValue().getAndSet(0L);
            if (count > 0) {
                logger.error("{}, count = {}", log, count);
            }
        }
    }
}
