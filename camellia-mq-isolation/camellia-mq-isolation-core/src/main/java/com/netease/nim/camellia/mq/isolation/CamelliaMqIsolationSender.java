package com.netease.nim.camellia.mq.isolation;

import com.netease.nim.camellia.mq.isolation.config.SenderConfig;
import com.netease.nim.camellia.mq.isolation.domain.MqIsolationMsg;
import com.netease.nim.camellia.mq.isolation.domain.MqIsolationMsgPacket;
import com.netease.nim.camellia.mq.isolation.domain.PacketSerializer;
import com.netease.nim.camellia.mq.isolation.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.mq.MqSender;
import com.netease.nim.camellia.mq.isolation.mq.SenderResult;
import com.netease.nim.camellia.mq.isolation.stats.SenderBizStats;
import com.netease.nim.camellia.tools.cache.CamelliaLoadingCache;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/2/4
 */
public class CamelliaMqIsolationSender implements MqIsolationSender {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMqIsolationSender.class);

    private final MqSender mqSender;
    private final MqIsolationController controller;
    private final CamelliaLoadingCache<CacheKey, List<MqInfo>> selectMqInfoCache;
    private final ConcurrentHashMap<String, CamelliaStatisticsManager> statsMap = new ConcurrentHashMap<>();

    public CamelliaMqIsolationSender(SenderConfig senderConfig) {
        this.mqSender = senderConfig.getMqSender();
        this.controller = senderConfig.getController();
        this.selectMqInfoCache = new CamelliaLoadingCache.Builder<CacheKey, List<MqInfo>>()
                .initialCapacity(senderConfig.getCacheCapacity())
                .maxCapacity(senderConfig.getCacheCapacity())
                .expireMillis(senderConfig.getCacheExpireSeconds() * 1000L)
                .build(key -> {
                    List<MqInfo> mqInfos;
                    try {
                        mqInfos = controller.selectMqInfo(key.getNamespace(), key.getBidId());
                    } catch (Exception e) {
                        logger.error("select mq info error, use fast mq info backup", e);
                        mqInfos = controller.getMqIsolationConfig(key.getNamespace()).getFast();
                    }
                    if (mqInfos == null || mqInfos.isEmpty()) {
                        mqInfos = controller.getMqIsolationConfig(key.getNamespace()).getFast();
                    }
                    return mqInfos;
                });
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-mq-isolation-sender-stats"))
                .scheduleAtFixedRate(this::reportStats, senderConfig.getReportIntervalSeconds(), senderConfig.getReportIntervalSeconds(), TimeUnit.SECONDS);
    }


    @Override
    public SenderResult send(MqIsolationMsg msg) {
        byte[] data = PacketSerializer.marshal(newPacket(msg));
        MqInfo mqInfo = selectMqInfo(msg);
        try {
            return mqSender.send(mqInfo, data);
        } finally {
            stats(msg.getNamespace(), msg.getBizId());
        }
    }

    private MqIsolationMsgPacket newPacket(MqIsolationMsg msg) {
        MqIsolationMsgPacket packet = new MqIsolationMsgPacket();
        packet.setMsg(msg);
        packet.setMsgId(UUID.randomUUID().toString().replace("-", ""));
        packet.setMsg(msg);
        long now = System.currentTimeMillis();
        packet.setMsgPushMqTime(now);
        packet.setMsgCreateTime(now);
        packet.setRetry(0);
        return packet;
    }

    private void stats(String namespace, String bizId) {
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

    private MqInfo selectMqInfo(MqIsolationMsg msg) {
        List<MqInfo> list = selectMqInfoCache.get(new CacheKey(msg.getNamespace(), msg.getBizId()));
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("select mqInfo error");
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        try {
            int index = ThreadLocalRandom.current().nextInt(list.size());
            return list.get(index);
        } catch (Exception e) {
            return list.get(0);
        }
    }

    private static class CacheKey {
        private final String namespace;
        private final String bidId;

        public CacheKey(String namespace, String bidId) {
            this.namespace = namespace;
            this.bidId = bidId;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getBidId() {
            return bidId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(namespace, cacheKey.namespace) && Objects.equals(bidId, cacheKey.bidId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, bidId);
        }
    }

}
