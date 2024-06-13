package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KvStorageSpendStats;
import com.netease.nim.camellia.redis.proxy.util.QuantileCollector;
import com.netease.nim.camellia.redis.proxy.util.QuantileCollectorPool;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2024/6/13
 */
public class KvStorageMonitor {

    private static final Logger logger = LoggerFactory.getLogger(KvStorageMonitor.class);

    private static final ConcurrentHashMap<String, LongAdder> spendCountMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> spendTotalMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, QuantileCollector> quantileMap = new ConcurrentHashMap<>();

    public static void update(String name, String method, long spend) {
        String key = name + "|" + method;
        try {
            CamelliaMapUtils.computeIfAbsent(spendCountMap, key, k -> new LongAdder()).increment();
            CamelliaMapUtils.computeIfAbsent(spendTotalMap, key, k -> new LongAdder()).add(spend);
            QuantileCollector collector = CamelliaMapUtils.computeIfAbsent(quantileMap, key,
                    k -> QuantileCollectorPool.borrowQuantileCollector());
            collector.update((int)(spend / 10000));//0.00ms
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static List<KvStorageSpendStats> collect() {
        try {
            List<KvStorageSpendStats> list = new ArrayList<>();
            ConcurrentHashMap<String, LongAdder> spendCountMap = KvStorageMonitor.spendCountMap;
            ConcurrentHashMap<String, LongAdder> spendTotalMap = KvStorageMonitor.spendTotalMap;
            ConcurrentHashMap<String, QuantileCollector> quantileMap = KvStorageMonitor.quantileMap;

            for (Map.Entry<String, LongAdder> entry : spendCountMap.entrySet()) {
                String key = entry.getKey();
                String[] split = key.split("\\|");
                String name = split[0];
                String method = split[1];
                long count = entry.getValue().sumThenReset();
                if (count == 0) continue;
                double avgSpendMs = 0;
                LongAdder nanoSum = spendTotalMap.get(key);
                long sum;
                if (nanoSum != null) {
                    sum = nanoSum.sumThenReset();
                    avgSpendMs = sum / (1000000.0 * count);
                }
                KvStorageSpendStats stats = new KvStorageSpendStats();
                stats.setName(name);
                stats.setMethod(method);
                stats.setCount(count);
                stats.setAvgSpendMs(avgSpendMs);

                QuantileCollector collector = quantileMap.get(key);
                if (collector != null) {
                    QuantileCollector.QuantileValue quantileValue = collector.getQuantileValueAndReset();
                    stats.setSpendMsP50(quantileValue.getP50() / 100.0);
                    stats.setSpendMsP75(quantileValue.getP75() / 100.0);
                    stats.setSpendMsP90(quantileValue.getP90() / 100.0);
                    stats.setSpendMsP95(quantileValue.getP95() / 100.0);
                    stats.setSpendMsP99(quantileValue.getP99() / 100.0);
                    stats.setSpendMsP999(quantileValue.getP999() / 100.0);
                    stats.setMaxSpendMs(quantileValue.getMax() / 100.0);
                }
                list.add(stats);
            }
            for (Map.Entry<String, QuantileCollector> entry : quantileMap.entrySet()) {
                QuantileCollector collector = entry.getValue();
                collector.reset();
                QuantileCollectorPool.returnQuantileCollector(collector);
            }
            quantileMap.clear();
            return list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
