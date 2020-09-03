package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.proxy.netty.ServerStatus;
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

    private static final int MAX_MAP_SIZE = 10000;
    private static final long PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS = 1000;
    private static ConcurrentHashMap<String, AtomicLong> logMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> lastPrintStackTraceMap = new ConcurrentHashMap<>();
    static {
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("error-log-collector"))
                .scheduleAtFixedRate(ErrorLogCollector::print, 10, 10, TimeUnit.SECONDS);
    }

    public static void collect(Class clazz, String log) {
        if (logMap.size() < MAX_MAP_SIZE) {
            AtomicLong count = logMap.computeIfAbsent(clazz.getName() + ":" + log, k -> new AtomicLong(0L));
            count.incrementAndGet();
        } else {
            logger.error(clazz.getName() + ":" + log);
        }
    }

    public static void collect(Class clazz, String log, Throwable e) {
        collect(clazz, log);
        if (lastPrintStackTraceMap.size() < MAX_MAP_SIZE) {
            AtomicLong lastPrintStackTraceTime = lastPrintStackTraceMap.computeIfAbsent(clazz.getName() + ":" + log, k -> new AtomicLong(0L));
            long lastTime = lastPrintStackTraceTime.get();
            if (ServerStatus.getCurrentTimeMillis() - lastTime > PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS) {
                boolean printTrace = lastPrintStackTraceTime.compareAndSet(lastTime, ServerStatus.getCurrentTimeMillis());
                if (printTrace) {
                    logger.error(clazz.getName() + ":" + log, e);
                }
            }
        }
    }

    private static void print() {
        try {
            ConcurrentHashMap<String, AtomicLong> printMap = logMap;
            logMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, AtomicLong> entry : printMap.entrySet()) {
                String log = entry.getKey();
                long count = entry.getValue().getAndSet(0L);
                if (count > 0) {
                    logger.error("{}, count = {}", log, count);
                }
            }
            lastPrintStackTraceMap.clear();
        } catch (Exception e) {
            logger.error("error log print error", e);
        }
    }
}
