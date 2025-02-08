package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.LocalStorageRunToCompletionStats;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2024/9/12
 */
public class LocalStorageRunToCompletionMonitor {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageRunToCompletionMonitor.class);

    private static final ConcurrentHashMap<String, LongAdder> hit_map = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> not_hit_map = new ConcurrentHashMap<>();

    public static void update(String command, boolean hit) {
        if (!ProxyMonitorCollector.isMonitorEnable()) return;
        LongAdder counter;
        if (hit) {
            counter = CamelliaMapUtils.computeIfAbsent(hit_map, command, k -> new LongAdder());
        } else {
            counter = CamelliaMapUtils.computeIfAbsent(not_hit_map, command, k -> new LongAdder());
        }
        counter.increment();
    }

    public static List<LocalStorageRunToCompletionStats> collect() {
        try {
            Set<String> keys = new HashSet<>();
            keys.addAll(hit_map.keySet());
            keys.addAll(not_hit_map.keySet());
            List<LocalStorageRunToCompletionStats> list = new ArrayList<>();
            for (String command : keys) {
                LocalStorageRunToCompletionStats stats = new LocalStorageRunToCompletionStats();
                stats.setCommand(command);
                LongAdder longAdder1 = hit_map.get(command);
                long hit = longAdder1 == null ? 0 : longAdder1.sumThenReset();
                LongAdder longAdder2 = not_hit_map.get(command);
                long notHit = longAdder2 == null ? 0 : longAdder2.sumThenReset();
                stats.setHit(hit);
                stats.setNotHit(notHit);
                if (hit + notHit > 0) {
                    stats.setHitRate((double) hit / (hit + notHit));
                    list.add(stats);
                }
            }
            return list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
