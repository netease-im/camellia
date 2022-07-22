package com.netease.nim.camellia.delayqueue.server;

import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2022/7/21
 */
public class CamelliaDelayMsgCounter {
    private final LongAdder sendMsg = new LongAdder();
    private final LongAdder pullMsg = new LongAdder();
    private final LongAdder deleteMsg = new LongAdder();
    private final LongAdder ackMsg = new LongAdder();
    private final LongAdder getMsg = new LongAdder();
    private final LongAdder triggerMsgReady = new LongAdder();
    private final LongAdder triggerMsgEndLife = new LongAdder();
    private final LongAdder triggerMsgTimeout = new LongAdder();

    public void sendMsg(long count) {
        sendMsg.add(count);
    }

    public void pullMsg(long count) {
        pullMsg.add(count);
    }

    public void deleteMsg(long count) {
        deleteMsg.add(count);
    }

    public void ackMsg(long count) {
        ackMsg.add(count);
    }

    public void getMsg(long count) {
        getMsg.add(count);
    }

    public void triggerMsgReady(long count) {
        triggerMsgReady.add(count);
    }

    public void triggerMsgEndLife(long count) {
        triggerMsgEndLife.add(count);
    }

    public void triggerMsgTimeout(long count) {
        triggerMsgTimeout.add(count);
    }

    public long getSendMsg() {
        return sendMsg.sum();
    }

    public long getPullMsg() {
        return pullMsg.sum();
    }

    public long getDeleteMsg() {
        return deleteMsg.sum();
    }

    public long getAckMsg() {
        return ackMsg.sum();
    }

    public long getGetMsg() {
        return getMsg.sum();
    }

    public long getTriggerMsgReady() {
        return triggerMsgReady.sum();
    }

    public long getTriggerMsgEndLife() {
        return triggerMsgEndLife.sum();
    }

    public long getTriggerMsgTimeout() {
        return triggerMsgTimeout.sum();
    }
}
