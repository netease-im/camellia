package com.netease.nim.camellia.mq.isolation.core;

import com.netease.nim.camellia.mq.isolation.core.config.SenderConfig;
import com.netease.nim.camellia.mq.isolation.core.domain.MqIsolationMsg;
import com.netease.nim.camellia.mq.isolation.core.domain.MqIsolationMsgPacket;
import com.netease.nim.camellia.mq.isolation.core.domain.PacketSerializer;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.MqSender;
import com.netease.nim.camellia.mq.isolation.core.mq.SenderResult;
import com.netease.nim.camellia.mq.isolation.core.stats.model.BizKey;
import com.netease.nim.camellia.mq.isolation.core.stats.SenderBizStatsCollector;
import com.netease.nim.camellia.tools.cache.CamelliaLoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2024/2/4
 */
public class CamelliaMqIsolationSender implements MqIsolationSender {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMqIsolationSender.class);

    private final MqSender mqSender;
    private final MqIsolationController controller;
    private final CamelliaLoadingCache<BizKey, List<MqInfo>> selectMqInfoCache;
    private final SenderBizStatsCollector collector;

    public CamelliaMqIsolationSender(SenderConfig senderConfig) {
        this.mqSender = senderConfig.getMqSender();
        this.controller = senderConfig.getController();
        this.selectMqInfoCache = new CamelliaLoadingCache.Builder<BizKey, List<MqInfo>>()
                .initialCapacity(senderConfig.getCacheCapacity())
                .maxCapacity(senderConfig.getCacheCapacity())
                .expireMillis(senderConfig.getCacheExpireSeconds() * 1000L)
                .build(key -> {
                    List<MqInfo> mqInfos;
                    try {
                        mqInfos = controller.selectMqInfo(key.getNamespace(), key.getBizId());
                    } catch (Exception e) {
                        logger.error("select mq info error, use fast mq info backup", e);
                        mqInfos = controller.getMqIsolationConfig(key.getNamespace()).getLevelInfoList().get(0).getMqInfoList();
                    }
                    if (mqInfos == null || mqInfos.isEmpty()) {
                        mqInfos = controller.getMqIsolationConfig(key.getNamespace()).getLevelInfoList().get(0).getMqInfoList();
                    }
                    return mqInfos;
                });
        this.collector = new SenderBizStatsCollector(controller);
    }

    @Override
    public SenderResult send(MqIsolationMsg msg) {
        byte[] data = PacketSerializer.marshal(newPacket(msg));
        MqInfo mqInfo = selectMqInfo(msg);
        try {
            return mqSender.send(mqInfo, data);
        } finally {
            collector.stats(msg.getNamespace(), msg.getBizId());
        }
    }

    @Override
    public SenderResult sendSpecifyMqInfo(MqIsolationMsg msg, MqInfo mqInfo) {
        byte[] data = PacketSerializer.marshal(newPacket(msg));
        try {
            return mqSender.send(mqInfo, data);
        } finally {
            collector.stats(msg.getNamespace(), msg.getBizId());
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

    private MqInfo selectMqInfo(MqIsolationMsg msg) {
        List<MqInfo> list = selectMqInfoCache.get(new BizKey(msg.getNamespace(), msg.getBizId()));
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

}
