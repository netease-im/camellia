package com.netease.nim.camellia.mq.isolation;

import com.netease.nim.camellia.mq.isolation.mq.MqInfo;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/2/6
 */
public interface MqIsolationConsumer {

    /**
     * 消费者，从mq中取到消息后，调用本接口，去处理消息
     * 会进行数据统计并做自动的隔离和重试
     * @param mqInfo mqInfo
     * @param data 数据
     * @return 结果，true表示正常处理了（不代表业务成功），false表示服务出现了异常
     */
    CompletableFuture<Boolean> onMsg(MqInfo mqInfo, byte[] data);

}
