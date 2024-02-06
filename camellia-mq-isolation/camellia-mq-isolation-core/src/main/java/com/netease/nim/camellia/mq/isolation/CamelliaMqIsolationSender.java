package com.netease.nim.camellia.mq.isolation;

import com.netease.nim.camellia.mq.isolation.domain.MqIsolationMsg;
import com.netease.nim.camellia.mq.isolation.domain.MqIsolationMsgPacket;
import com.netease.nim.camellia.mq.isolation.domain.PacketSerializer;
import com.netease.nim.camellia.mq.isolation.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.mq.MqSender;
import com.netease.nim.camellia.mq.isolation.mq.SenderResult;

/**
 * Created by caojiajun on 2024/2/4
 */
public class CamelliaMqIsolationSender implements MqIsolationSender {

    private MqSender mqSender;

    @Override
    public SenderResult send(MqIsolationMsg msg) {
        MqIsolationMsgPacket packet = new MqIsolationMsgPacket();
        byte[] data = PacketSerializer.marshal(packet);
        MqInfo mqInfo = selectMqInfo(msg);
        return mqSender.send(mqInfo, data);
    }

    private MqInfo selectMqInfo(MqIsolationMsg msg) {
        return new MqInfo();
    }

}
