package com.netease.nim.camellia.mq.isolation.mq;

/**
 * Created by caojiajun on 2024/2/6
 */
public interface MqSender {

    /**
     * 实际的mq消息发送者，由上层提供，可以基于不同的mq实现（如kafka、rabbitmq、rocketmq等）
     * @param mqInfo mqInfo
     * @param data 数据
     * @return 发送结果
     */
    SenderResult send(MqInfo mqInfo, byte[] data);

}
