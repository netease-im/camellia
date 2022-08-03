package com.netease.nim.camellia.redis.toolkit.freq;

/**
 * Created by caojiajun on 2022/8/1
 */
public class CamelliaFreqConfig {

    private long checkTime;//检查周期，单位ms
    private long threshold;//阈值，一个周期内的最大请求数
    private long banTime;//超过阈值后的惩罚屏蔽时间，如果不需要惩罚机制，则填0

    public long getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(long checkTime) {
        this.checkTime = checkTime;
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public long getBanTime() {
        return banTime;
    }

    public void setBanTime(long banTime) {
        this.banTime = banTime;
    }
}
