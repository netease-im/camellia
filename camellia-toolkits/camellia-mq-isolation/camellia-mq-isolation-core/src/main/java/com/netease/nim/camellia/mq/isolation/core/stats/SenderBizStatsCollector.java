package com.netease.nim.camellia.mq.isolation.core.stats;

import com.netease.nim.camellia.core.util.CollectionSplitUtil;
import com.netease.nim.camellia.mq.isolation.core.MqIsolationController;
import com.netease.nim.camellia.mq.isolation.core.domain.SenderHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.env.MqIsolationEnv;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStats;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStatsRequest;
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
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2024/2/7
 */
public class SenderBizStatsCollector {

    private static final Logger logger = LoggerFactory.getLogger(SenderBizStatsCollector.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-mq-isolation-sender-collector"));

    private final ConcurrentHashMap<String, CamelliaStatisticsManager> statsMap = new ConcurrentHashMap<>();
    private final MqIsolationController controller;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();

    public SenderBizStatsCollector(MqIsolationController controller) {
        this.controller = controller;
    }

    public void stats(String namespace, String bizId) {
        CamelliaStatisticsManager manager = CamelliaMapUtils.computeIfAbsent(statsMap, namespace, s -> new CamelliaStatisticsManager());
        manager.update(bizId, 1);
        if (futureMap.containsKey(namespace)) {
            return;
        }
        futureMap.computeIfAbsent(namespace,
                n -> {
                    int intervalSeconds = controller.getMqIsolationConfig(namespace).getSenderStatsIntervalSeconds();
                    return scheduler.scheduleAtFixedRate(() -> reportStats(namespace, intervalSeconds), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
                });
    }

    private void reportStats(String namespace, int intervalSeconds) {
        try {
            List<SenderBizStats> statsList = new ArrayList<>();
            CamelliaStatisticsManager manager = statsMap.get(namespace);
            if (manager == null) {
                return;
            }
            long now = (System.currentTimeMillis() / intervalSeconds) * intervalSeconds;
            Map<String, CamelliaStatsData> map = manager.getStatsDataAndReset();
            for (Map.Entry<String, CamelliaStatsData> dataEntry : map.entrySet()) {
                String bizId = dataEntry.getKey();
                CamelliaStatsData data = dataEntry.getValue();
                SenderBizStats stats = new SenderBizStats();
                stats.setNamespace(namespace);
                stats.setBizId(bizId);
                stats.setCount(data.getCount());
                stats.setTimestamp(now);
                statsList.add(stats);
            }
            if (statsList.isEmpty()) {
                return;
            }
            //report stats
            List<List<SenderBizStats>> split = CollectionSplitUtil.split(statsList, 100);
            for (List<SenderBizStats> list : split) {
                SenderBizStatsRequest request = new SenderBizStatsRequest();
                request.setInstanceId(MqIsolationEnv.instanceId);
                request.setList(list);
                controller.reportSenderBizStats(request);
            }
            //heartbeat
            SenderHeartbeat heartbeat = new SenderHeartbeat();
            heartbeat.setInstanceId(MqIsolationEnv.instanceId);
            heartbeat.setHost(MqIsolationEnv.host);
            heartbeat.setNamespace(namespace);
            heartbeat.setTimestamp(System.currentTimeMillis());
            controller.senderHeartbeat(heartbeat);
        } catch (Exception e) {
            logger.error("report stats error", e);
        }
    }
}
