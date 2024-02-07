package com.netease.nim.camellia.mq.isolation.core.stats;

import com.netease.nim.camellia.mq.isolation.core.MqIsolationController;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStats;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/2/7
 */
public class SenderBizStatsCollector {

    private static final Logger logger = LoggerFactory.getLogger(SenderBizStatsCollector.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-mq-isolation-sender-collector"));

    private final ConcurrentHashMap<String, CamelliaStatisticsManager> statsMap = new ConcurrentHashMap<>();
    private final MqIsolationController controller;

    public SenderBizStatsCollector(MqIsolationController controller, int reportIntervalSeconds) {
        this.controller = controller;
        scheduler.scheduleAtFixedRate(this::reportStats, reportIntervalSeconds, reportIntervalSeconds, TimeUnit.SECONDS);
    }

    public void stats(String namespace, String bizId) {
        CamelliaStatisticsManager manager = CamelliaMapUtils.computeIfAbsent(statsMap, namespace, s -> new CamelliaStatisticsManager());
        manager.update(bizId, 1);
    }

    private void reportStats() {
        try {
            List<SenderBizStats> statsList = new ArrayList<>();
            for (Map.Entry<String, CamelliaStatisticsManager> entry : statsMap.entrySet()) {
                String namespace = entry.getKey();
                Map<String, CamelliaStatsData> map = entry.getValue().getStatsDataAndReset();
                for (Map.Entry<String, CamelliaStatsData> dataEntry : map.entrySet()) {
                    String bizId = dataEntry.getKey();
                    CamelliaStatsData data = dataEntry.getValue();
                    SenderBizStats stats = new SenderBizStats();
                    stats.setNamespace(namespace);
                    stats.setBizId(bizId);
                    stats.setCount(data.getCount());
                    statsList.add(stats);
                }
            }
            controller.reportSenderBizStats(statsList);
        } catch (Exception e) {
            logger.error("report stats error", e);
        }
    }
}
