package com.netease.nim.camellia.mq.isolation.core.stats;

import com.netease.nim.camellia.core.util.CollectionSplitUtil;
import com.netease.nim.camellia.mq.isolation.core.MqIsolationController;
import com.netease.nim.camellia.mq.isolation.core.env.MqIsolationEnv;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStats;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStatsRequest;
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
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2024/2/7
 */
public class ConsumerBizStatsCollector {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerBizStatsCollector.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-mq-isolation-consumer-collector"));

    private final ConcurrentHashMap<String, CamelliaStatisticsManager> statsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CamelliaStatisticsManager> failedMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();

    private final MqIsolationController controller;

    public ConsumerBizStatsCollector(MqIsolationController controller) {
        this.controller = controller;
    }

    public void stats(String namespace, String bizId, boolean success, long spendMs) {
        CamelliaStatisticsManager manager = CamelliaMapUtils.computeIfAbsent(statsMap, namespace, s -> new CamelliaStatisticsManager());
        manager.update(bizId, spendMs);
        if (!success) {
            CamelliaStatisticsManager failedManager = CamelliaMapUtils.computeIfAbsent(failedMap, namespace, s -> new CamelliaStatisticsManager());
            failedManager.update(bizId, 1);
        }
        if (futureMap.containsKey(namespace)) {
            return;
        }
        futureMap.computeIfAbsent(namespace, s -> {
            int intervalSeconds = controller.getMqIsolationConfig(namespace).getConsumerStatsIntervalSeconds();
            return scheduler.scheduleAtFixedRate(() -> reportStats(namespace, intervalSeconds), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        });
    }

    private void reportStats(String namespace, int intervalSeconds) {
        try {
            CamelliaStatisticsManager manager = statsMap.get(namespace);
            if (manager == null) {
                return;
            }
            long now = (System.currentTimeMillis() / intervalSeconds) * intervalSeconds;
            List<ConsumerBizStats> statsList = new ArrayList<>();
            CamelliaStatisticsManager failedManager = failedMap.get(namespace);
            Map<String, CamelliaStatsData> failedDataMap;
            if (failedManager != null) {
                failedDataMap = failedManager.getStatsDataAndReset();
            } else {
                failedDataMap = new HashMap<>();
            }
            Map<String, CamelliaStatsData> map = manager.getStatsDataAndReset();
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
                stats.setTimestamp(now);
                statsList.add(stats);
            }
            List<List<ConsumerBizStats>> split = CollectionSplitUtil.split(statsList, 100);
            for (List<ConsumerBizStats> list : split) {
                ConsumerBizStatsRequest request = new ConsumerBizStatsRequest();
                request.setInstanceId(MqIsolationEnv.instanceId);
                request.setList(list);
                controller.reportConsumerBizStats(request);
            }
        } catch (Exception e) {
            logger.error("report stats error", e);
        }
    }
}
