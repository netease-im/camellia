package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class HotKeyConfig {
    private final long checkMillis;
    private final int checkCacheMaxCapacity;
    private final long checkThreshold;
    private final int maxHotKeyCount;

    public HotKeyConfig(long checkMillis, int checkCacheMaxCapacity, long checkThreshold, int maxHotKeyCount) {
        this.checkMillis = checkMillis;
        this.checkCacheMaxCapacity = checkCacheMaxCapacity;
        this.checkThreshold = checkThreshold;
        this.maxHotKeyCount = maxHotKeyCount;
    }

    public long getCheckMillis() {
        return checkMillis;
    }

    public int getCheckCacheMaxCapacity() {
        return checkCacheMaxCapacity;
    }

    public long getCheckThreshold() {
        return checkThreshold;
    }

    public int getMaxHotKeyCount() {
        return maxHotKeyCount;
    }
}
