package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.UpstreamRedisSpendStats;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientAddr;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.util.QuantileCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class UpstreamRedisSpendTimeMonitor {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamRedisSpendTimeMonitor.class);

    private static ConcurrentHashMap<String, LongAdder> spendCountMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, LongAdder> spendTotalMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, QuantileCollector> quantileMap = new ConcurrentHashMap<>();

    public static void incr(RedisClientAddr addr, long spendNanoTime) {
        try {
            CamelliaMapUtils.computeIfAbsent(spendCountMap, addr.getUrl(), k -> new LongAdder()).increment();
            CamelliaMapUtils.computeIfAbsent(spendTotalMap, addr.getUrl(), k -> new LongAdder()).add(spendNanoTime);
            QuantileCollector collector = CamelliaMapUtils.computeIfAbsent(quantileMap, addr.getUrl(),
                    k -> new QuantileCollector());
            collector.update((int)(spendNanoTime / 10000));//0.00ms
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static List<UpstreamRedisSpendStats> collect() {
        List<UpstreamRedisSpendStats> list = new ArrayList<>();
        ConcurrentHashMap<String, LongAdder> spendCountMap = UpstreamRedisSpendTimeMonitor.spendCountMap;
        ConcurrentHashMap<String, LongAdder> spendTotalMap = UpstreamRedisSpendTimeMonitor.spendTotalMap;
        ConcurrentHashMap<String, QuantileCollector> quantileMap = UpstreamRedisSpendTimeMonitor.quantileMap;

        UpstreamRedisSpendTimeMonitor.spendCountMap = new ConcurrentHashMap<>();
        UpstreamRedisSpendTimeMonitor.spendTotalMap = new ConcurrentHashMap<>();
        UpstreamRedisSpendTimeMonitor.quantileMap = new ConcurrentHashMap<>();

        for (Map.Entry<String, LongAdder> entry : spendCountMap.entrySet()) {
            String key = entry.getKey();
            long count = entry.getValue().sumThenReset();
            if (count == 0) continue;
            double avgSpendMs = 0;
            LongAdder nanoSum = spendTotalMap.get(key);
            long sum;
            if (nanoSum != null) {
                sum = nanoSum.sumThenReset();
                avgSpendMs = sum / (1000000.0 * count);
            }
            UpstreamRedisSpendStats upstreamRedisSpendStats = new UpstreamRedisSpendStats();
            upstreamRedisSpendStats.setAddr(PasswordMaskUtils.maskAddr(key));
            upstreamRedisSpendStats.setCount(count);
            upstreamRedisSpendStats.setAvgSpendMs(avgSpendMs);

            QuantileCollector collector = quantileMap.get(key);
            if (collector != null) {
                QuantileCollector.QuantileValue quantileValue = collector.getQuantileValueAndReset();
                upstreamRedisSpendStats.setSpendMsP50(quantileValue.getP50() / 100.0);
                upstreamRedisSpendStats.setSpendMsP75(quantileValue.getP75() / 100.0);
                upstreamRedisSpendStats.setSpendMsP90(quantileValue.getP90() / 100.0);
                upstreamRedisSpendStats.setSpendMsP95(quantileValue.getP95() / 100.0);
                upstreamRedisSpendStats.setSpendMsP99(quantileValue.getP99() / 100.0);
                upstreamRedisSpendStats.setSpendMsP999(quantileValue.getP999() / 100.0);
                upstreamRedisSpendStats.setMaxSpendMs(quantileValue.getMax() / 100.0);
            }
            list.add(upstreamRedisSpendStats);
        }
        return list;
    }
}
