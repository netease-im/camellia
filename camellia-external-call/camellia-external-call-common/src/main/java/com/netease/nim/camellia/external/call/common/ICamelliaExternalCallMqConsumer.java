package com.netease.nim.camellia.external.call.common;

import java.util.List;

/**
 * Created by caojiajun on 2023/2/24
 */
public interface ICamelliaExternalCallMqConsumer {

    /**
     * 业务方需要调用这个接口获取需要监听的mq地址和topic，并获取到消息后调用onMsg方法给到ICamelliaExternalCallMqConsumer
     * @return mqInfo list
     */
    List<MqInfo> acquireMqInfo();

    /**
     * 业务方请自行从mq中取出消息，并调用本方法
     * @param mqInfo mqInfo
     * @param data data
     * @return 成功/失败
     */
    boolean onMsg(MqInfo mqInfo, byte[] data);

}
