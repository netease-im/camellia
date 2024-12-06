package com.netease.nim.camellia.redis.proxy.plugin.permission.model;

/**
 *
 * Created by anhdt9 on 2022/22/12
 */
public class RateLimitConf {
    long checkMillis;
    long maxCount;

    public RateLimitConf(long checkMillis, long maxCount) {
        this.checkMillis = checkMillis;
        this.maxCount = maxCount;
    }

    public long getCheckMillis() {
        return checkMillis;
    }

    public void setCheckMillis(long checkMillis) {
        this.checkMillis = checkMillis;
    }

    public long getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(long maxCount) {
        this.maxCount = maxCount;
    }
}
