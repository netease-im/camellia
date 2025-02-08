package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.LocalStorageCountStats;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2025/2/8
 */
public class LocalStorageCountMonitor {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageCountMonitor.class);

    private static ConcurrentHashMap<String, LongAdder> countMap = new ConcurrentHashMap<>();

    public static void count(String item) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        CamelliaMapUtils.computeIfAbsent(countMap, item, k -> new LongAdder()).increment();
    }

    public static List<LocalStorageCountStats> collect() {
        try {
            ConcurrentHashMap<String, LongAdder> countMap = LocalStorageCountMonitor.countMap;
            LocalStorageCountMonitor.countMap = new ConcurrentHashMap<>();
            List<LocalStorageCountStats> list = new ArrayList<>();
            for (Map.Entry<String, LongAdder> entry : countMap.entrySet()) {
                LocalStorageCountStats stats = new LocalStorageCountStats();
                stats.setItem(entry.getKey());
                stats.setCount(entry.getValue().sumThenReset());
                list.add(stats);
            }
            return list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
