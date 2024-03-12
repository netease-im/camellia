package com.netease.nim.camellia.mq.isolation.core;

import com.netease.nim.camellia.mq.isolation.core.domain.MqIsolationMsg;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.SenderResult;

/**
 * Created by caojiajun on 2024/2/6
 */
public interface MqIsolationSender {

    /**
     * 业务消息的生产方，调用本接口，把消息发送出去
     * 内部会做数据统计，并做不同的路由分发
     * @param msg 消息
     * @return 发送结果
     */
    SenderResult send(MqIsolationMsg msg);


    /**
     * 指定mqInfo的方式去发送消息
     * @param msg 消息
     * @param mqInfo mqInfo
     * @return 发送结果
     */
    SenderResult sendSpecifyMqInfo(MqIsolationMsg msg, MqInfo mqInfo);

}
