package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/24
 */
public interface MqSender {

    /**
     * 业务方自行实现投递给mq的一个sender
     * @param mqInfo mqInfo
     * @param data 数据
     * @return 发送结果
     */
    boolean send(MqInfo mqInfo, byte[] data);
}
