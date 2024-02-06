package com.netease.nim.camellia.mq.isolation;

import com.netease.nim.camellia.mq.isolation.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.stats.ConsumerBizStats;
import com.netease.nim.camellia.mq.isolation.stats.SenderBizStats;

import java.util.List;

/**
 * Created by caojiajun on 2024/2/6
 */
public interface MqIsolationController {

    MqIsolationConfig getMqIsolationConfig(String namespace);

    void reportConsumerBizStats(List<ConsumerBizStats> list);

    void reportSenderBizStats(List<SenderBizStats> list);

    List<MqInfo> selectMqInfo(String namespace, String bizId);

    List<MqInfo> defaultMqInfo(String namespace);

}
