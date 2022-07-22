package com.netease.nim.camellia.delayqueue.sdk;


import com.netease.nim.camellia.delayqueue.common.conf.CamelliaDelayQueueConstants;

/**
 * Created by caojiajun on 2022/7/12
 */
public class CamelliaDelayMsgListenerConfig {

    private long ackTimeoutMillis = CamelliaDelayQueueConstants.ackTimeoutMillis;
    private int pullBatch = CamelliaDelayQueueConstants.pullBatch;
    private int pullIntervalTimeMillis = CamelliaDelayQueueConstants.pullIntervalTimeMillis;//轮询间隔，单位ms，默认100ms
    private int pullThreads = CamelliaDelayQueueConstants.pullThreads;//pull线程池大小，默认1

    public long getAckTimeoutMillis() {
        return ackTimeoutMillis;
    }

    public void setAckTimeoutMillis(long ackTimeoutMillis) {
        this.ackTimeoutMillis = ackTimeoutMillis;
    }

    public int getPullBatch() {
        return pullBatch;
    }

    public void setPullBatch(int pullBatch) {
        this.pullBatch = pullBatch;
    }

    public int getPullIntervalTimeMillis() {
        return pullIntervalTimeMillis;
    }

    public void setPullIntervalTimeMillis(int pullIntervalTimeMillis) {
        this.pullIntervalTimeMillis = pullIntervalTimeMillis;
    }

    public int getPullThreads() {
        return pullThreads;
    }

    public void setPullThreads(int pullThreads) {
        this.pullThreads = pullThreads;
    }
}
