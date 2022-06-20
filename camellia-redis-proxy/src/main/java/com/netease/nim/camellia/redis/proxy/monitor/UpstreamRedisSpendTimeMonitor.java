package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.command.async.RedisClientAddr;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.util.MaxValue;
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
    private static ConcurrentHashMap<String, MaxValue> spendMaxMap = new ConcurrentHashMap<>();

    public static void incrUpstreamRedisSpendTime(RedisClientAddr addr, long spendNanoTime) {
        if (!RedisMonitor.isUpstreamRedisSpendTimeMonitorEnable()) return;
        try {
            CamelliaMapUtils.computeIfAbsent(spendCountMap, addr.getUrl(), k -> new LongAdder()).increment();
            CamelliaMapUtils.computeIfAbsent(spendTotalMap, addr.getUrl(), k -> new LongAdder()).add(spendNanoTime);
            MaxValue maxValue = CamelliaMapUtils.computeIfAbsent(spendMaxMap, addr.getUrl(), k -> new MaxValue());
            maxValue.update(spendNanoTime);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static List<Stats.UpstreamRedisSpendStats> calc() {
        List<Stats.UpstreamRedisSpendStats> list = new ArrayList<>();
        try {
            ConcurrentHashMap<String, LongAdder> spendCountMap = UpstreamRedisSpendTimeMonitor.spendCountMap;
            ConcurrentHashMap<String, LongAdder> spendTotalMap = UpstreamRedisSpendTimeMonitor.spendTotalMap;
            ConcurrentHashMap<String, MaxValue> spendMaxMap = UpstreamRedisSpendTimeMonitor.spendMaxMap;

            UpstreamRedisSpendTimeMonitor.spendCountMap = new ConcurrentHashMap<>();
            UpstreamRedisSpendTimeMonitor.spendTotalMap = new ConcurrentHashMap<>();
            UpstreamRedisSpendTimeMonitor.spendMaxMap = new ConcurrentHashMap<>();

            for (Map.Entry<String, LongAdder> entry : spendCountMap.entrySet()) {
                String key = entry.getKey();
                long count = entry.getValue().sumThenReset();
                if (count == 0) continue;
                MaxValue nanoMax = spendMaxMap.get(key);
                double maxSpendMs = 0;
                if (nanoMax != null) {
                    maxSpendMs = nanoMax.getAndSet(0) / 1000000.0;
                }
                double avgSpendMs = 0;
                LongAdder nanoSum = spendTotalMap.get(key);
                long sum;
                if (nanoSum != null) {
                    sum = nanoSum.sumThenReset();
                    avgSpendMs = sum / (1000000.0 * count);
                }
                Stats.UpstreamRedisSpendStats upstreamRedisSpendStats = new Stats.UpstreamRedisSpendStats();
                upstreamRedisSpendStats.setAddr(PasswordMaskUtils.maskAddr(key));
                upstreamRedisSpendStats.setCount(count);
                upstreamRedisSpendStats.setAvgSpendMs(avgSpendMs);
                upstreamRedisSpendStats.setMaxSpendMs(maxSpendMs);
                list.add(upstreamRedisSpendStats);
            }
            return list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return list;
        }
    }
}
