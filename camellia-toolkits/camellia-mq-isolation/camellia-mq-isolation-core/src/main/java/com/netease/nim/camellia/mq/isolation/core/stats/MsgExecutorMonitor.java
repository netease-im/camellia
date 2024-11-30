package com.netease.nim.camellia.mq.isolation.core.stats;

import com.netease.nim.camellia.mq.isolation.core.executor.MsgExecutor;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ExecutorStats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/2/23
 */
public class MsgExecutorMonitor {

    private static final ConcurrentHashMap<String, MsgExecutor> map = new ConcurrentHashMap<>();

    public static void register(MsgExecutor executor) {
        map.put(executor.getName(), executor);
    }

    public static List<ExecutorStats> getStats() {
        List<ExecutorStats> list = new ArrayList<>();
        map.forEach((key, value) -> list.add(value.getStats()));
        return list;
    }
}
