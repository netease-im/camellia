package com.netease.nim.camellia.redis.proxy.mq.common;

public interface MqPackSender {

    boolean send(MqPack pack) throws Exception;
}
