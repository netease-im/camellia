package com.netease.nim.camellia.mq.isolation;

import com.netease.nim.camellia.mq.isolation.domain.MqIsolationMsg;
import com.netease.nim.camellia.mq.isolation.mq.SenderResult;

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

}
