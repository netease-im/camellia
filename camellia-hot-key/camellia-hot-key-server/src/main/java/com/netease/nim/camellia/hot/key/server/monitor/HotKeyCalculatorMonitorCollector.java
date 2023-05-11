package com.netease.nim.camellia.hot.key.server.monitor;

import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyCalculatorMonitorCollector {

    private static final ConcurrentHashMap<Long, HotKeyCalculatorMonitor> map = new ConcurrentHashMap<>();

    public static void register(long id, HotKeyCalculatorMonitor monitor) {
        map.put(id, monitor);
    }

    public static TrafficStats collect() {
        TrafficStats merge = new TrafficStats();
        Map<String, AtomicLong> countMap = new HashMap<>();
        for (Map.Entry<Long, HotKeyCalculatorMonitor> entry : map.entrySet()) {
            TrafficStats trafficStats = entry.getValue().collect();
            merge.setTotal(merge.getTotal() + trafficStats.getTotal());
            for (TrafficStats.Stats stats : trafficStats.getStatsList()) {
                String key = stats.getNamespace() + "|" + stats.getType().getValue();
                AtomicLong count = CamelliaMapUtils.computeIfAbsent(countMap, key, k -> new AtomicLong(0));
                count.addAndGet(stats.getCount());
            }
        }
        List<TrafficStats.Stats> statsList = new ArrayList<>();
        for (Map.Entry<String, AtomicLong> entry : countMap.entrySet()) {
            String key = entry.getKey();
            int index = key.lastIndexOf("\\|");
            String namespace = key.substring(0, index);
            TrafficStats.Type type = TrafficStats.Type.getByValue(Integer.parseInt(key.substring(index + 1)));
            TrafficStats.Stats stats = new TrafficStats.Stats();
            stats.setNamespace(namespace);
            stats.setType(type);
            stats.setCount(entry.getValue().get());
            statsList.add(stats);
        }
        merge.setStatsList(statsList);
        return merge;
    }
}
