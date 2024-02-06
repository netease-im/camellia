package com.netease.nim.camellia.mq.isolation.domain;

import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2024/2/6
 */
public class PacketSerializer {

    public static byte[] marshal(MqIsolationMsgPacket packet) {
        return JSONObject.toJSONString(packet).getBytes(StandardCharsets.UTF_8);
    }

    public static MqIsolationMsgPacket unmarshal(byte[] data) {
        return JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), MqIsolationMsgPacket.class);
    }
}
