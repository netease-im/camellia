package com.netease.nim.camellia.redis.proxy.plugin.monitor;

import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.monitor.model.BidBgroupSpendStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.SpendStats;
import com.netease.nim.camellia.redis.proxy.util.MaxValue;
import com.netease.nim.camellia.redis.proxy.util.PValueCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 命令的耗时
 */
public class CommandSpendMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CommandSpendMonitor.class);

    private static ConcurrentHashMap<String, LongAdder> commandSpendCountMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, LongAdder> commandSpendTotalMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, MaxValue> commandSpendMaxMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, PValueCollector> commandPValueMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, PValueCollector> bidBgroupCommandPValueMap = new ConcurrentHashMap<>();

    public static void incr(Long bid, String bgroup, String command, long spendNanoTime) {
        try {
            String key = bid + "|" + bgroup + "|" + command;
            CamelliaMapUtils.computeIfAbsent(commandSpendCountMap, key, k -> new LongAdder()).increment();
            CamelliaMapUtils.computeIfAbsent(commandSpendTotalMap, key, k -> new LongAdder()).add(spendNanoTime);
            MaxValue maxValue = CamelliaMapUtils.computeIfAbsent(commandSpendMaxMap, key, k -> new MaxValue());
            maxValue.update(spendNanoTime);
            int time = (int)(spendNanoTime / 10000);//0.00ms
            PValueCollector collector1 = CamelliaMapUtils.computeIfAbsent(bidBgroupCommandPValueMap, key,
                    k -> new PValueCollector(ProxyMonitorCollector.getMonitorPValueExpectMaxSpendMs() * 100));
            collector1.update(time);
            PValueCollector collector2 = CamelliaMapUtils.computeIfAbsent(commandPValueMap, command,
                    k -> new PValueCollector(ProxyMonitorCollector.getMonitorPValueExpectMaxSpendMs() * 100));
            collector2.update(time);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static class CommandSpendStats {
        public List<BidBgroupSpendStats> bidBgroupSpendStatsList = new ArrayList<>();
        public List<SpendStats> spendStatsList = new ArrayList<>();
    }

    public static CommandSpendStats collect() {

        ConcurrentHashMap<String, LongAdder> commandSpendCountMap = CommandSpendMonitor.commandSpendCountMap;
        ConcurrentHashMap<String, LongAdder> commandSpendTotalMap = CommandSpendMonitor.commandSpendTotalMap;
        ConcurrentHashMap<String, MaxValue> commandSpendMaxMap = CommandSpendMonitor.commandSpendMaxMap;
        ConcurrentHashMap<String, PValueCollector> commandPValueMap = CommandSpendMonitor.commandPValueMap;
        ConcurrentHashMap<String, PValueCollector> bidBgroupCommandPValueMap = CommandSpendMonitor.bidBgroupCommandPValueMap;

        CommandSpendMonitor.commandSpendCountMap = new ConcurrentHashMap<>();
        CommandSpendMonitor.commandSpendTotalMap = new ConcurrentHashMap<>();
        CommandSpendMonitor.commandSpendMaxMap = new ConcurrentHashMap<>();
        CommandSpendMonitor.commandPValueMap = new ConcurrentHashMap<>();
        CommandSpendMonitor.bidBgroupCommandPValueMap = new ConcurrentHashMap<>();

        List<BidBgroupSpendStats> list = new ArrayList<>();
        ConcurrentHashMap<String, AtomicLong> spendSumMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, SpendStats> spendStatsMap = new ConcurrentHashMap<>();
        for (Map.Entry<String, LongAdder> entry : commandSpendCountMap.entrySet()) {
            String key = entry.getKey();
            long count = entry.getValue().sumThenReset();
            if (count == 0) continue;
            MaxValue nanoMax = commandSpendMaxMap.get(key);
            double maxSpendMs = 0;
            if (nanoMax != null) {
                maxSpendMs = nanoMax.getAndSet(0) / 1000000.0;
            }
            double avgSpendMs = 0;
            LongAdder nanoSum = commandSpendTotalMap.get(key);
            long sum = 0;
            if (nanoSum != null) {
                sum = nanoSum.sumThenReset();
                avgSpendMs = sum / (1000000.0 * count);
            }
            String[] split = key.split("\\|");
            Long bid = null;
            if (!split[0].equals("null")) {
                bid = Long.parseLong(split[0]);
            }
            String bgroup = null;
            if (!split[1].equals("null")) {
                bgroup = split[1];
            }
            String command = split[2];
            BidBgroupSpendStats bidBgroupSpendStats = new BidBgroupSpendStats();
            bidBgroupSpendStats.setBid(bid);
            bidBgroupSpendStats.setBgroup(bgroup);
            bidBgroupSpendStats.setCommand(command);
            bidBgroupSpendStats.setAvgSpendMs(avgSpendMs);
            bidBgroupSpendStats.setMaxSpendMs(maxSpendMs);
            bidBgroupSpendStats.setCount(count);

            PValueCollector collector = bidBgroupCommandPValueMap.get(key);
            if (collector != null) {
                PValueCollector.PValue pValue = collector.getPValueAndReset();
                bidBgroupSpendStats.setSpendMsP50(pValue.getP50() / 100.0);
                bidBgroupSpendStats.setSpendMsP75(pValue.getP75() / 100.0);
                bidBgroupSpendStats.setSpendMsP90(pValue.getP90() / 100.0);
                bidBgroupSpendStats.setSpendMsP95(pValue.getP95() / 100.0);
                bidBgroupSpendStats.setSpendMsP99(pValue.getP99() / 100.0);
                bidBgroupSpendStats.setSpendMsP999(pValue.getP999() / 100.0);
            }

            list.add(bidBgroupSpendStats);

            SpendStats spendStats = CamelliaMapUtils.computeIfAbsent(spendStatsMap, command, string -> {
                SpendStats bean = new SpendStats();
                bean.setCommand(string);
                return bean;
            });
            spendStats.setCount(spendStats.getCount() + bidBgroupSpendStats.getCount());
            if (bidBgroupSpendStats.getMaxSpendMs() > spendStats.getMaxSpendMs()) {
                spendStats.setMaxSpendMs(bidBgroupSpendStats.getMaxSpendMs());
            }
            CamelliaMapUtils.computeIfAbsent(spendSumMap, command, k -> new AtomicLong()).addAndGet(sum);
        }
        for (Map.Entry<String, SpendStats> entry : spendStatsMap.entrySet()) {
            String command = entry.getKey();
            SpendStats spendStats = entry.getValue();
            AtomicLong sum = spendSumMap.get(command);
            if (sum == null) {
                spendStats.setAvgSpendMs(0.0);
            } else {
                spendStats.setAvgSpendMs(sum.get() / (1000000.0 * spendStats.getCount()));
            }
            PValueCollector collector = commandPValueMap.get(command);
            if (collector != null) {
                PValueCollector.PValue pValue = collector.getPValueAndReset();
                spendStats.setSpendMsP50(pValue.getP50() / 100.0);
                spendStats.setSpendMsP75(pValue.getP75() / 100.0);
                spendStats.setSpendMsP90(pValue.getP90() / 100.0);
                spendStats.setSpendMsP95(pValue.getP95() / 100.0);
                spendStats.setSpendMsP99(pValue.getP99() / 100.0);
                spendStats.setSpendMsP999(pValue.getP999() / 100.0);
            }
        }
        CommandSpendStats commandSpendStats = new CommandSpendStats();
        commandSpendStats.bidBgroupSpendStatsList = list;
        commandSpendStats.spendStatsList = new ArrayList<>(spendStatsMap.values());
        return commandSpendStats;
    }
}
