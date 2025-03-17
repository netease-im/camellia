package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KvLoadCacheStats;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2025/3/14
 */
public class KvLoadCacheMonitor {

    private static final Logger logger = LoggerFactory.getLogger(KvLoadCacheMonitor.class);

    private static final Map<String, LongAdder> map = new ConcurrentHashMap<>();

    public static void update(String namespace, String command) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        CamelliaMapUtils.computeIfAbsent(map, namespace + "|" + command, k -> new LongAdder()).increment();
    }

    public static List<KvLoadCacheStats> collect() {
        try {
            List<KvLoadCacheStats> list = new ArrayList<>();
            for (Map.Entry<String, LongAdder> entry : map.entrySet()) {
                String[] split = entry.getKey().split("\\|");
                String namespace = split[0];
                String command = split[1];
                long count = entry.getValue().sumThenReset();
                if (count <= 0) {
                    continue;
                }
                KvLoadCacheStats stats = new KvLoadCacheStats();
                stats.setNamespace(namespace);
                stats.setCommand(command);
                stats.setCount(count);
                list.add(stats);
            }
            return list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

}
