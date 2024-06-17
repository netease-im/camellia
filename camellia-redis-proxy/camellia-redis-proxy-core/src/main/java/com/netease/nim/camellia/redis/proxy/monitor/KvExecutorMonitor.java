package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KvExecutorStats;
import com.netease.nim.camellia.redis.proxy.util.MpscSlotHashExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KvExecutorMonitor {

    private static final ConcurrentHashMap<String, MpscSlotHashExecutor> map = new ConcurrentHashMap<>();

    public static void register(String namespace, MpscSlotHashExecutor executor) {
        map.put(namespace, executor);
    }

    public static List<KvExecutorStats> collect() {
        List<KvExecutorStats> list = new ArrayList<>();
        for (Map.Entry<String, MpscSlotHashExecutor> entry : map.entrySet()) {
            KvExecutorStats stats = new KvExecutorStats();
            stats.setName(entry.getKey());
            stats.setPending(entry.getValue().getQueueSize());
            list.add(stats);
        }
        return list;
    }
}
