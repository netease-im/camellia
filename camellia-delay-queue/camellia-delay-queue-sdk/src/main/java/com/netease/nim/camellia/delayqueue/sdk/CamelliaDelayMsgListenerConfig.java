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
    private int consumeThreads = CamelliaDelayQueueConstants.consumeThreads;
    private boolean longPollingEnable = CamelliaDelayQueueConstants.longPollingEnable;//是否启用长轮询
    private long longPollingTimeoutMillis = CamelliaDelayQueueConstants.longPollingTimeoutMillis;//长轮询的超时

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

    public int getConsumeThreads() {
        return consumeThreads;
    }

    public void setConsumeThreads(int consumeThreads) {
        this.consumeThreads = consumeThreads;
    }

    public boolean isLongPollingEnable() {
        return longPollingEnable;
    }

    public void setLongPollingEnable(boolean longPollingEnable) {
        this.longPollingEnable = longPollingEnable;
    }

    public long getLongPollingTimeoutMillis() {
        return longPollingTimeoutMillis;
    }

    public void setLongPollingTimeoutMillis(long longPollingTimeoutMillis) {
        this.longPollingTimeoutMillis = longPollingTimeoutMillis;
    }
}
