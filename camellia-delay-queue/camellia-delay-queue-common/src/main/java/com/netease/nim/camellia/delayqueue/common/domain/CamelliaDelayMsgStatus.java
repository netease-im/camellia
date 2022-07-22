package com.netease.nim.camellia.delayqueue.common.domain;

/**
 * Created by caojiajun on 2022/7/14
 */
public enum CamelliaDelayMsgStatus {
    WAITING(1, false),//消息处于等待状态
    READY(2, false),//消息已经到期，等待消费
    CONSUMING(3, false),//消息正在被消费，尚未收到ack
    CONSUME_OK(4, true),//消息已经被成功消费，收到了ack
    EXPIRE(5, true),//消息过期了，没有被消费过
    RETRY_EXHAUST(6, true),//消息被消费过，但是没有正确收到ack，最终由于超过了最大重试次数或者超过了ttl而被丢弃
    DELETE(7, true),//消息被主动删除了
    ;

    private final int value;
    private final boolean endLife;

    CamelliaDelayMsgStatus(int value, boolean endLife) {
        this.value = value;
        this.endLife = endLife;
    }

    public int getValue() {
        return value;
    }

    public boolean isEndLife() {
        return endLife;
    }

    public static CamelliaDelayMsgStatus getByValue(int value) {
        for (CamelliaDelayMsgStatus status : CamelliaDelayMsgStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }
}
