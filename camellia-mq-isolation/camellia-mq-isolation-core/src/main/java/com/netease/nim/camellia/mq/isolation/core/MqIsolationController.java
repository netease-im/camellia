package com.netease.nim.camellia.mq.isolation.core;

import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStats;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStats;

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
     * @param list 数据
     */
    void reportConsumerBizStats(List<ConsumerBizStats> list);

    /**
     * 上报生产端统计数据
     * @param list 数据
     */
    void reportSenderBizStats(List<SenderBizStats> list);

    /**
     * 获取动态路由
     * @param namespace namespace
     * @param bizId bizId
     * @return 路由结果
     */
    List<MqInfo> selectMqInfo(String namespace, String bizId);

}
