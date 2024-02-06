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
    private final SenderConfig senderConfig;

    private CamelliaLoadingCache<String, List<MqInfo>> defaultMqInfo;
    private CamelliaLoadingCache<CacheKey, List<MqInfo>> mqInfoCache;

    private final ConcurrentHashMap<String, CamelliaStatisticsManager> manager = new ConcurrentHashMap<>();

    public CamelliaMqIsolationSender(SenderConfig senderConfig) {
        this.senderConfig = senderConfig;
        this.mqSender = senderConfig.getMqSender();
        this.controller = senderConfig.getController();
        init();
    }

    private void init() {
        defaultMqInfo = new CamelliaLoadingCache.Builder<String, List<MqInfo>>()
                .initialCapacity(senderConfig.getNamespaceCacheCapacity())
                .maxCapacity(senderConfig.getNamespaceCacheCapacity())
                .expireMillis(senderConfig.getCacheExpireSeconds() * 1000L)
                .build(controller::defaultMqInfo);

        mqInfoCache = new CamelliaLoadingCache.Builder<CacheKey, List<MqInfo>>()
                .initialCapacity(senderConfig.getCacheCapacity())
                .maxCapacity(senderConfig.getCacheCapacity())
                .expireMillis(senderConfig.getCacheExpireSeconds() * 1000L)
                .build(key -> controller.selectMqInfo(key.getNamespace(), key.getBidId()));
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-mq-isolation-sender-stats"))
                .scheduleAtFixedRate(this::reportStats, senderConfig.getReportIntervalSeconds(), senderConfig.getReportIntervalSeconds(), TimeUnit.SECONDS);
    }


    @Override
    public SenderResult send(MqIsolationMsg msg) {
        MqIsolationMsgPacket packet = new MqIsolationMsgPacket();
        packet.setMsg(msg);
        packet.setMsgId(UUID.randomUUID().toString().replace("-", ""));
        packet.setMsg(msg);
        long now = System.currentTimeMillis();
        packet.setMsgPushMqTime(now);
        packet.setMsgCreateTime(now);
        packet.setRetry(0);
        byte[] data = PacketSerializer.marshal(packet);
        MqInfo mqInfo = selectMqInfo(msg);
        try {
            return mqSender.send(mqInfo, data);
        } finally {
            stats(msg.getNamespace(), msg.getBizId());
        }
    }

    private void stats(String namespace, String bizId) {
        CamelliaStatisticsManager statisticsManager = CamelliaMapUtils.computeIfAbsent(manager, namespace, s -> new CamelliaStatisticsManager());
        statisticsManager.update(bizId, 1);
    }

    private void reportStats() {
        try {
            List<SenderBizStats> statsList = new ArrayList<>();
            for (Map.Entry<String, CamelliaStatisticsManager> entry : manager.entrySet()) {
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
        List<MqInfo> mqInfos;
        try {
            mqInfos = mqInfoCache.get(new CacheKey(msg.getNamespace(), msg.getBizId()));
        } catch (Exception e) {
            mqInfos = defaultMqInfo.get(msg.getNamespace());
        }
        if (mqInfos == null || mqInfos.isEmpty()) {
            mqInfos = defaultMqInfo.get(msg.getNamespace());
        }
        if (mqInfos == null || mqInfos.isEmpty()) {
            throw new IllegalArgumentException("select mqInfo error");
        }
        if (mqInfos.size() == 1) {
            return mqInfos.get(0);
        }
        try {
            int index = ThreadLocalRandom.current().nextInt(mqInfos.size());
            return mqInfos.get(index);
        } catch (Exception e) {
            return mqInfos.get(0);
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
