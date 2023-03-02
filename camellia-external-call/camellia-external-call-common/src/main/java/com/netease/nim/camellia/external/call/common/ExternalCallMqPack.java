package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/24
 */
public class ExternalCallMqPack {
    private String isolationKey;
    private byte[] data;
    private long createTime;
    private int retry;//重试次数，第一次是0，之后每一次+1
    private boolean highPriority;//是否属于高优

    public String getIsolationKey() {
        return isolationKey;
    }

    public void setIsolationKey(String isolationKey) {
        this.isolationKey = isolationKey;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    public void setHighPriority(boolean highPriority) {
        this.highPriority = highPriority;
    }
}
