package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/28
 */
public class BizContext {
    private String isolationKey;
    private int retry;//重试次数，第一次是0，之后每一次+1

    public String getIsolationKey() {
        return isolationKey;
    }

    public void setIsolationKey(String isolationKey) {
        this.isolationKey = isolationKey;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }
}
