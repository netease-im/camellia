package com.netease.nim.camellia.redis.proxy.plugin.permission.model;

/**
 * @author anhdt9
 * @since 22/12/2022
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
