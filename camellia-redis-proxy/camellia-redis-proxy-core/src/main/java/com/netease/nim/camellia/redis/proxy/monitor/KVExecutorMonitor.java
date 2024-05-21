package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KVExecutorStats;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.RedisKvClientExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KVExecutorMonitor {

    private static final ConcurrentHashMap<String, KvGcExecutor> map = new ConcurrentHashMap<>();

    public static void register(String namespace, KvGcExecutor gcExecutor) {
        map.put(namespace, gcExecutor);
    }

    public static List<KVExecutorStats> collect() {
        List<KVExecutorStats> list = new ArrayList<>();
        for (Map.Entry<String, KvGcExecutor> entry : map.entrySet()) {
            KVExecutorStats stats = new KVExecutorStats();
            stats.setName("gc-" + entry.getKey());
            stats.setPending(entry.getValue().pending());
            list.add(stats);
        }
        KVExecutorStats stats = new KVExecutorStats();
        stats.setName("command");
        stats.setPending(RedisKvClientExecutor.getInstance().getExecutor().getQueueSize());
        list.add(stats);
        return list;
    }
}
