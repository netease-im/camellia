package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
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

    /**
     * The maximum size of the cached map
     */
    private static final String CONF_MAX_CACHE_SIZE = "error.log.collect.cache.max.size";
    private static final int DEFAULT_MAX_CACHE_SIZE = 10000;
    /**
     * Minimum interval for printing error logs
     */
    private static final String CONF_PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS = "error.log.collect.print.trace.min.interval.millis";
    private static final long DEFAULT_PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS = 1000;
    /**
     * Log container map，key= {@link ErrorLogCollector#getKey(Class, String)} value = count
     */
    private static ConcurrentHashMap<String, AtomicLong> logMap = new ConcurrentHashMap<>();
    /**
     * Record the time when the log was last printed，key= {@link ErrorLogCollector#getKey(Class, String)} value = count
     */
    private static final ConcurrentHashMap<String, AtomicLong> lastPrintStackTraceMap = new ConcurrentHashMap<>();

    // Print the error log every 10 seconds
    static {
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("error-log-collector"))
                .scheduleAtFixedRate(ErrorLogCollector::print, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * If cache hasn't reached the maximum value, the method will collect the log string into {@link ErrorLogCollector#logMap}
     * Otherwise, print it.
     *
     * @param clazz class
     * @param log   logStr
     */
    public static void collect(Class clazz, String log) {
        if (logMap.size() < ProxyDynamicConf.getInt(CONF_MAX_CACHE_SIZE, DEFAULT_MAX_CACHE_SIZE)) {
            AtomicLong count = CamelliaMapUtils.computeIfAbsent(logMap, getKey(clazz, log), k -> new AtomicLong(0L));
            count.incrementAndGet();
        } else {
            logger.error(getKey(clazz, log));
        }
    }

    /**
     * Get the key in the map
     */
    private static String getKey(Class clazz, String log) {
        return clazz.getName() + Utils.COLON + log;
    }

    /**
     * Collect logs.
     * If the log printing exceeds the printing interval, it will be printed directly,
     * that is to say, there is no need to print through scheduled tasks
     *
     * @param clazz class
     * @param log   log string
     * @param e     exception
     */
    public static void collect(Class clazz, String log, Throwable e) {
        collect(clazz, log);
        if (lastPrintStackTraceMap.size() < ProxyDynamicConf.getInt(CONF_MAX_CACHE_SIZE, DEFAULT_MAX_CACHE_SIZE)) {
            String key = getKey(clazz, log);
            AtomicLong lastPrintStackTraceTime = CamelliaMapUtils.computeIfAbsent(lastPrintStackTraceMap, key, k -> new AtomicLong(0L));
            long lastTime = lastPrintStackTraceTime.get();
            // If the time since the last print exceeds the time interval in the configuration, it will be printed
            if (TimeCache.currentMillis - lastTime > ProxyDynamicConf.getLong(CONF_PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS, DEFAULT_PRINT_ERROR_TRACE_MIN_INTERVAL_MILLIS)) {
                // the CAS operation is used to ensure that it is printed only once
                boolean printTrace = lastPrintStackTraceTime.compareAndSet(lastTime, TimeCache.currentMillis);
                if (printTrace) {
                    logger.error(key, e);
                    logMap.remove(key);
                }
            }
        }
    }

    /**
     * clear and print logs in {@link ErrorLogCollector#logMap} by traversing
     */
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
