package com.netease.nim.camellia.redis.toolkit.freq;

/**
 * 示例一：
 * checkTime=1000，threshold=10，banTime=2000，delayBanEnable=true
 * 表示1s内最多10次请求，如果超过了，则2s内不允许请求，如果还有请求，2s会一直顺延，直到连续2s内没有新的请求进来，频控才会取消
 *
 * 示例二：
 * checkTime=1000，threshold=10，banTime=2000，delayBanEnable=false
 * 表示1s内最多10次请求，如果超过了，则2s内不允许请求，2s之后直接频控自动取消
 *
 * 示例三：
 * checkTime=1000，threshold=10，banTime=0
 * 表示1s内最多10次请求，如果超过了，则返回失败，等当前这个周期（1s）过去了，则频控自动取消
 * Created by caojiajun on 2022/8/1
 */
public class CamelliaFreqConfig {

    private long checkTime;//检查周期，单位ms
    private long threshold;//阈值，一个周期内的最大请求数
    private long banTime;//超过阈值后的惩罚屏蔽时间
    private boolean delayBanEnable;//超过阈值后进入屏蔽时间，此时如果有新请求过来，是否要顺延屏蔽时间

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

    public boolean isDelayBanEnable() {
        return delayBanEnable;
    }

    public void setDelayBanEnable(boolean delayBanEnable) {
        this.delayBanEnable = delayBanEnable;
    }
}
