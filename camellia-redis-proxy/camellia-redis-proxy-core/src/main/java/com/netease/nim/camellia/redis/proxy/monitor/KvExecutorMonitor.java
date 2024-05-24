package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KvExecutorStats;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.KvExecutors;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KvExecutorMonitor {

    private static final ConcurrentHashMap<String, KvGcExecutor> map = new ConcurrentHashMap<>();

    public static void register(String namespace, KvGcExecutor gcExecutor) {
        map.put(namespace, gcExecutor);
    }

    public static List<KvExecutorStats> collect() {
        List<KvExecutorStats> list = new ArrayList<>();
        for (Map.Entry<String, KvGcExecutor> entry : map.entrySet()) {
            KvExecutorStats stats = new KvExecutorStats();
            stats.setName("gc-" + entry.getKey());
            stats.setPending(entry.getValue().pending());
            list.add(stats);
        }
        {
            KvExecutorStats stats = new KvExecutorStats();
            stats.setName("command");
            stats.setPending(KvExecutors.getInstance().getCommandExecutor().getQueueSize());
            list.add(stats);
        }
        {
            KvExecutorStats stats = new KvExecutorStats();
            stats.setName("async-write");
            stats.setPending(KvExecutors.getInstance().getAsyncWriteExecutor().getQueueSize());
            list.add(stats);
        }
        return list;
    }
}
