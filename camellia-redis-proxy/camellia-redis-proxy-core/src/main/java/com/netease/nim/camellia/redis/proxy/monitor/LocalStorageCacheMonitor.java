package com.netease.nim.camellia.redis.proxy.monitor;


import com.netease.nim.camellia.redis.proxy.monitor.model.LocalStorageCacheStats;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class LocalStorageCacheMonitor {

    public static enum Type {
        mem_table,
        row_cache,
        block_cache,
        disk,
        ;
    }

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageCacheMonitor.class);

    private static final ConcurrentHashMap<String, Counter> map = new ConcurrentHashMap<>();

    public static void update(Type type, String operation) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        Counter counter = CamelliaMapUtils.computeIfAbsent(map, operation, s -> new Counter());
        if (type == Type.mem_table) {
            counter.memTable.increment();
        } else if (type == Type.row_cache) {
            counter.rowCache.increment();
        } else if (type == Type.block_cache) {
            counter.blockCache.increment();
        } else if (type == Type.disk) {
            counter.disk.increment();
        }
    }

    public static List<LocalStorageCacheStats> collect() {
        List<LocalStorageCacheStats> list = new ArrayList<>();
        try {
            for (Map.Entry<String, Counter> entry : map.entrySet()) {
                String operation = entry.getKey();
                LocalStorageCacheStats stats = new LocalStorageCacheStats();
                stats.setOperation(operation);
                Counter counter = entry.getValue();
                long c1 = counter.memTable.sumThenReset();
                stats.setMemTable(c1);
                long c2 = counter.rowCache.sumThenReset();
                stats.setRowCache(c2);
                long c3 = counter.blockCache.sumThenReset();
                stats.setBlockCache(c3);
                long c4 = counter.disk.sumThenReset();
                stats.setDisk(c4);
                //
                long sum = c1 + c2 + c3 + c4;
                stats.setMemTableHit(c1 * 1.0 / sum);
                stats.setRowCacheHit(c2 * 1.0 / sum);
                stats.setBlockCacheHit(c3 * 1.0 / sum);
                stats.setDiskHit(c4 * 1.0 / sum);
                list.add(stats);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return list;
    }

    private static class Counter {
        LongAdder memTable = new LongAdder();
        LongAdder rowCache = new LongAdder();
        LongAdder blockCache = new LongAdder();
        LongAdder disk = new LongAdder();
    }

}
