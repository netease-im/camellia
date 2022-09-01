package com.netease.nim.camellia.delayqueue.server;

/**
 * Created by caojiajun on 2022/9/1
 */
public interface CamelliaDelayMsgReadyCallback {

    void callback(String topic);
}
