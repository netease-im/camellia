package com.netease.nim.camellia.redis.proxy.command.async.spendtime;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandSpendTimeConfig {

    private final long slowCommandThresholdMillisTime;
    private final SlowCommandCallback slowCommandCallback;

    public CommandSpendTimeConfig(long slowCommandThresholdMillisTime,
                                  SlowCommandCallback slowCommandCallback) {
        this.slowCommandThresholdMillisTime = slowCommandThresholdMillisTime;
        this.slowCommandCallback = slowCommandCallback;
    }

    public long getSlowCommandThresholdMillisTime() {
        return slowCommandThresholdMillisTime;
    }

    public SlowCommandCallback getSlowCommandCallback() {
        return slowCommandCallback;
    }
}
