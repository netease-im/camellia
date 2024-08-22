package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KvExecutorStats;
import com.netease.nim.camellia.redis.proxy.util.MpscSlotHashExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KvExecutorMonitor {

    private static final ConcurrentHashMap<String, MpscSlotHashExecutor> map1 = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ThreadPoolExecutor> map2 = new ConcurrentHashMap<>();

    public static void register(String name, MpscSlotHashExecutor executor) {
        map1.put(name, executor);
    }

    public static void register(String name, ThreadPoolExecutor executor) {
        map2.put(name, executor);
    }

    public static List<KvExecutorStats> collect() {
        List<KvExecutorStats> list = new ArrayList<>();
        for (Map.Entry<String, MpscSlotHashExecutor> entry : map1.entrySet()) {
            KvExecutorStats stats = new KvExecutorStats();
            stats.setName(entry.getKey());
            stats.setPending(entry.getValue().getQueueSize());
            list.add(stats);
        }
        for (Map.Entry<String, ThreadPoolExecutor> entry : map2.entrySet()) {
            KvExecutorStats stats = new KvExecutorStats();
            stats.setName(entry.getKey());
            stats.setPending(entry.getValue().getQueue().size());
            list.add(stats);
        }
        return list;
    }
}
