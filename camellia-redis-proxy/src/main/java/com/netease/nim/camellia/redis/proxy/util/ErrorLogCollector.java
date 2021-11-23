package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
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

    private static final String CONF_MAX_CACHE_SIZE = "error.log.collect.cache.max.size";
    private static final int DEFAULT_MAX_CACHE_SIZE = 10000;
    private static final String CONF_PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS = "error.log.collect.print.trace.min.interval.millis";
    private static final long DEFAULT_PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS = 1000;
    private static ConcurrentHashMap<String, AtomicLong> logMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> lastPrintStackTraceMap = new ConcurrentHashMap<>();
    static {
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("error-log-collector"))
                .scheduleAtFixedRate(ErrorLogCollector::print, 10, 10, TimeUnit.SECONDS);
    }

    public static void collect(Class clazz, String log) {
        if (logMap.size() < ProxyDynamicConf.getInt(CONF_MAX_CACHE_SIZE, DEFAULT_MAX_CACHE_SIZE)) {
            AtomicLong count = CamelliaMapUtils.computeIfAbsent(logMap, clazz.getName() + ":" + log, k -> new AtomicLong(0L));
            count.incrementAndGet();
        } else {
            logger.error(clazz.getName() + ":" + log);
        }
    }

    public static void collect(Class clazz, String log, Throwable e) {
        collect(clazz, log);
        if (lastPrintStackTraceMap.size() < ProxyDynamicConf.getInt(CONF_MAX_CACHE_SIZE, DEFAULT_MAX_CACHE_SIZE)) {
            AtomicLong lastPrintStackTraceTime = CamelliaMapUtils.computeIfAbsent(lastPrintStackTraceMap, clazz.getName() + ":" + log, k -> new AtomicLong(0L));
            long lastTime = lastPrintStackTraceTime.get();
            if (TimeCache.currentMillis - lastTime > ProxyDynamicConf.getLong(CONF_PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS, DEFAULT_PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS)) {
                boolean printTrace = lastPrintStackTraceTime.compareAndSet(lastTime, TimeCache.currentMillis);
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
