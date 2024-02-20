package com.netease.nim.camellia.mq.isolation.core;

import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStats;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStatsRequest;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStats;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStatsRequest;

import java.util.List;

/**
 * Created by caojiajun on 2024/2/6
 */
public interface MqIsolationController {

    /**
     * 获取MqIsolationConfig
     * @param namespace namespace
     * @return config
     */
    MqIsolationConfig getMqIsolationConfig(String namespace);

    /**
     * 上报消费端统计数据
     * @param request 数据
     */
    void reportConsumerBizStats(ConsumerBizStatsRequest request);

    /**
     * 上报生产端统计数据
     * @param request 数据
     */
    void reportSenderBizStats(SenderBizStatsRequest request);

    /**
     * 获取动态路由
     * @param namespace namespace
     * @param bizId bizId
     * @return 路由结果
     */
    List<MqInfo> selectMqInfo(String namespace, String bizId);

    /**
     * 心跳
     * @param namespace namespace
     * @param mqInfo mqInfo
     */
    void heartbeat(String namespace, MqInfo mqInfo);
}
