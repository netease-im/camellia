package com.netease.nim.camellia.mq.isolation.stats;

import com.netease.nim.camellia.mq.isolation.MqIsolationController;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/2/7
 */
public class ConsumerBizStatsCollector {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerBizStatsCollector.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-mq-isolation-consumer-collector"));

    private final ConcurrentHashMap<String, CamelliaStatisticsManager> statsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CamelliaStatisticsManager> failedMap = new ConcurrentHashMap<>();

    private final MqIsolationController controller;

    public ConsumerBizStatsCollector(MqIsolationController controller, int reportIntervalSeconds) {
        this.controller = controller;
        scheduler.scheduleAtFixedRate(this::reportStats, reportIntervalSeconds, reportIntervalSeconds, TimeUnit.SECONDS);
    }

    public void stats(String namespace, String bizId, boolean success, long spendMs) {
        CamelliaStatisticsManager manager = CamelliaMapUtils.computeIfAbsent(statsMap, namespace, s -> new CamelliaStatisticsManager());
        manager.update(bizId, spendMs);
        if (!success) {
            CamelliaStatisticsManager failedManager = CamelliaMapUtils.computeIfAbsent(failedMap, namespace, s -> new CamelliaStatisticsManager());
            failedManager.update(bizId, 1);
        }
    }

    private void reportStats() {
        try {
            List<ConsumerBizStats> statsList = new ArrayList<>();
            for (Map.Entry<String, CamelliaStatisticsManager> entry : statsMap.entrySet()) {
                String namespace = entry.getKey();
                CamelliaStatisticsManager failedManager = failedMap.get(namespace);
                Map<String, CamelliaStatsData> failedDataMap;
                if (failedManager != null) {
                    failedDataMap = failedManager.getStatsDataAndReset();
                } else {
                    failedDataMap = new HashMap<>();
                }
                Map<String, CamelliaStatsData> map = entry.getValue().getStatsDataAndReset();
                for (Map.Entry<String, CamelliaStatsData> dataEntry : map.entrySet()) {
                    String bizId = dataEntry.getKey();
                    CamelliaStatsData data = dataEntry.getValue();
                    ConsumerBizStats stats = new ConsumerBizStats();
                    stats.setNamespace(namespace);
                    stats.setBizId(bizId);
                    long total = data.getCount();
                    CamelliaStatsData failedData = failedDataMap.get(bizId);
                    long fail = failedData == null ? 0 : failedData.getCount();
                    long success = total - fail < 0 ? 0 : total - fail;
                    stats.setSuccess(success);
                    stats.setFail(fail);
                    stats.setSpendAvg(data.getAvg());
                    stats.setSpendMax(data.getMax());
                    stats.setP50(data.getP50());
                    stats.setP90(data.getP90());
                    stats.setP99(data.getP99());
                    statsList.add(stats);
                }
            }
            controller.reportConsumerBizStats(statsList);
        } catch (Exception e) {
            logger.error("report stats error", e);
        }
    }
}
