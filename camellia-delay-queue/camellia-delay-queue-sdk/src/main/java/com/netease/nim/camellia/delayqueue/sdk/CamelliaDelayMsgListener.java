package com.netease.nim.camellia.delayqueue.sdk;

import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsg;

/**
 * Created by caojiajun on 2022/7/7
 */
public interface CamelliaDelayMsgListener {

    /**
     * 延迟消息回调
     * @param delayMsg 消息
     * @return ack结果，如果true表示消息成功，如果false表示消费失败，会被重试
     */
    boolean onMsg(CamelliaDelayMsg delayMsg);
}
