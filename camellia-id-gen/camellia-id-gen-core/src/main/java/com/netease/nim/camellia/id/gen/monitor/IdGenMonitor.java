package com.netease.nim.camellia.id.gen.monitor;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;
import com.netease.nim.camellia.tools.sys.CpuUsageCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/12/26
 */
public class IdGenMonitor {

    private static final Logger logger = LoggerFactory.getLogger(IdGenMonitor.class);

    private static final AtomicBoolean initOk = new AtomicBoolean(false);

    private static final CamelliaStatisticsManager manager = new CamelliaStatisticsManager();
    private static CpuUsageCollector cpuUsageCollector;

    private static Stats stats = new Stats();

    public static void init(int intervalSeconds) {
        if (initOk.compareAndSet(false, true)) {
            cpuUsageCollector = new CpuUsageCollector(intervalSeconds);
            Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("id-gen-monitor"))
                    .scheduleAtFixedRate(IdGenMonitor::calc, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        } else {
            cpuUsageCollector = new CpuUsageCollector();
        }
    }

    private static void calc() {
        try {
            Stats stats1 = new Stats();
            List<Stats.UriStats> uriStatsList = new ArrayList<>();
            Map<String, CamelliaStatsData> dataMap = manager.getStatsDataAndReset();
            for (Map.Entry<String, CamelliaStatsData> entry : dataMap.entrySet()) {
                String[] split = entry.getKey().split("\\|");
                String uri = split[0];
                int code = Integer.parseInt(split[1]);
                Stats.UriStats uriStats = new Stats.UriStats();
                uriStats.setUri(uri);
                uriStats.setCode(code);
                uriStats.setCount(entry.getValue().getCount());
                uriStats.setSpendAvg(entry.getValue().getCount());
                uriStats.setSpendMax(entry.getValue().getMax());
                uriStats.setSpendP50(entry.getValue().getP50());
                uriStats.setSpendP90(entry.getValue().getP90());
                uriStats.setSpendP99(entry.getValue().getP99());
                uriStatsList.add(uriStats);
            }
            stats1.setStatsList(uriStatsList);
            IdGenMonitor.stats = stats1;
        } catch (Exception e) {
            logger.error("calc error", e);
        }
    }

    public static void update(String uri, long spendTime, int code) {
        manager.update(uri + "|" + code, spendTime);
    }

    public static Stats getStats() {
        return stats;
    }

    public static CpuUsageCollector getCpuUsageCollector() {
        return cpuUsageCollector;
    }
}
