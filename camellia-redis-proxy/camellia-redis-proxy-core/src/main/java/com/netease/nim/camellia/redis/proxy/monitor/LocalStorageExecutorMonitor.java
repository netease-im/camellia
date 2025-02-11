package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.LocalStorageExecutorStats;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.LocalStorageExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2025/2/8
 */
public class LocalStorageExecutorMonitor {

    public static List<LocalStorageExecutorStats> collect() {
        List<LocalStorageExecutorStats> list = new ArrayList<>();
        {
            LocalStorageExecutorStats stats = new LocalStorageExecutorStats();
            stats.setName("flush");
            stats.setPending(LocalStorageExecutors.getInstance().getFlushExecutor().getQueueSize());
            list.add(stats);
        }
        return list;
    }
}
