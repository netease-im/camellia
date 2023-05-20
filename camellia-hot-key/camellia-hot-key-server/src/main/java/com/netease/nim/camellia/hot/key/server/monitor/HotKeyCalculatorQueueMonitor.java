package com.netease.nim.camellia.hot.key.server.monitor;

import com.netease.nim.camellia.hot.key.server.calculate.HotKeyCalculatorQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyCalculatorQueueMonitor {

    private static final ConcurrentHashMap<Long, HotKeyCalculatorQueue> queueMap = new ConcurrentHashMap<>();

    public static void register(HotKeyCalculatorQueue queue) {
        queueMap.put(queue.getId(), queue);
    }

    public static QueueStats collect() {
        QueueStats queueStats = new QueueStats();
        queueStats.setQueueNum(queueMap.size());
        List<QueueStats.Stats> statsList = new ArrayList<>();
        for (Map.Entry<Long, HotKeyCalculatorQueue> entry : queueMap.entrySet()) {
            QueueStats.Stats stats = new QueueStats.Stats();
            stats.setId(entry.getKey());
            stats.setPendingSize(entry.getValue().pendingSize());
            stats.setDiscardCount(entry.getValue().discardCount());
            statsList.add(stats);
        }
        queueStats.setStatsList(statsList);
        return queueStats;
    }
}
