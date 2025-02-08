package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.LocalStorageTimeStats;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/2/8
 */
public class LocalStorageTimeMonitor {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageTimeMonitor.class);

    private static ConcurrentHashMap<String, TimeCollector> map = new ConcurrentHashMap<>();

    public static void time(String item, long time) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        if (time < 0) return;
        CamelliaMapUtils.computeIfAbsent(map, item, k -> new TimeCollector()).update(time);
    }

    public static List<LocalStorageTimeStats> collect() {
        try {
            ConcurrentHashMap<String, TimeCollector> map = LocalStorageTimeMonitor.map;
            LocalStorageTimeMonitor.map = new ConcurrentHashMap<>();

            List<LocalStorageTimeStats> list = new ArrayList<>();
            for (Map.Entry<String, TimeCollector> entry : map.entrySet()) {
                LocalStorageTimeStats stats = new LocalStorageTimeStats();
                stats.setItem(entry.getKey());
                stats.setTimeStats(entry.getValue().getStats());
                list.add(stats);
            }
            return list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
