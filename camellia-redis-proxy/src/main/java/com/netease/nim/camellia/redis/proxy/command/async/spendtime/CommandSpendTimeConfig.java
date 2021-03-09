package com.netease.nim.camellia.redis.proxy.command.async.spendtime;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandSpendTimeConfig {

    private long slowCommandThresholdMillisTime;
    private long slowCommandThresholdNanoTime;
    private final SlowCommandMonitorCallback slowCommandMonitorCallback;

    public CommandSpendTimeConfig(long slowCommandThresholdMillisTime,
                                  SlowCommandMonitorCallback slowCommandMonitorCallback) {
        this.slowCommandThresholdMillisTime = slowCommandThresholdMillisTime;
        this.slowCommandThresholdNanoTime = slowCommandThresholdMillisTime * 1000000L;
        this.slowCommandMonitorCallback = slowCommandMonitorCallback;
        ProxyDynamicConf.registerCallback(this::reloadSlowCommandThresholdMillisTime);
    }

    private void reloadSlowCommandThresholdMillisTime() {
        this.slowCommandThresholdMillisTime = ProxyDynamicConf.slowCommandThresholdMillisTime(this.slowCommandThresholdMillisTime);
        this.slowCommandThresholdNanoTime = this.slowCommandThresholdMillisTime * 1000000L;
    }

    public long getSlowCommandThresholdMillisTime() {
        return slowCommandThresholdMillisTime;
    }

    public long getSlowCommandThresholdNanoTime() {
        return slowCommandThresholdNanoTime;
    }

    public SlowCommandMonitorCallback getSlowCommandMonitorCallback() {
        return slowCommandMonitorCallback;
    }
}
