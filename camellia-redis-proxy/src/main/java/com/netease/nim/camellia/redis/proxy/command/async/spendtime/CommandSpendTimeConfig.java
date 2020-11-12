package com.netease.nim.camellia.redis.proxy.command.async.spendtime;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandSpendTimeConfig {

    private final long slowCommandThresholdMillisTime;
    private final SlowCommandMonitorCallback slowCommandMonitorCallback;

    public CommandSpendTimeConfig(long slowCommandThresholdMillisTime,
                                  SlowCommandMonitorCallback slowCommandMonitorCallback) {
        this.slowCommandThresholdMillisTime = slowCommandThresholdMillisTime;
        this.slowCommandMonitorCallback = slowCommandMonitorCallback;
    }

    public long getSlowCommandThresholdMillisTime() {
        return slowCommandThresholdMillisTime;
    }

    public SlowCommandMonitorCallback getSlowCommandMonitorCallback() {
        return slowCommandMonitorCallback;
    }
}
