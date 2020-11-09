package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.conf.Constants;


/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class CommandHotKeyCacheConfig {

    private long hotKeyCacheExpireMillis = Constants.Server.hotKeyCacheExpireMillis;
    private long hotKeyCacheMaxCapacity = Constants.Server.hotKeyCacheMaxCapacity;

    private long hotKeyCacheCounterCheckMillis = Constants.Server.hotKeyCacheCounterCheckMillis;
    private long hotKeyCacheCounterMaxCapacity = Constants.Server.hotKeyCacheCounterMaxCapacity;
    private long hotKeyCacheCounterCheckThreshold = Constants.Server.hotKeyCacheCounterCheckThreshold;
    private boolean hotKeyCacheNeedCacheNull = Constants.Server.hotKeyCacheNeedCacheNull;

    private HotKeyCacheKeyChecker hotKeyCacheKeyChecker;

    private long hotKeyCacheStatsCallbackIntervalSeconds = Constants.Server.hotKeyCacheStatsCallbackIntervalSeconds;
    private HotKeyCacheStatsCallback hotKeyCacheStatsCallback;

    public long getHotKeyCacheStatsCallbackIntervalSeconds() {
        return hotKeyCacheStatsCallbackIntervalSeconds;
    }

    public void setHotKeyCacheStatsCallbackIntervalSeconds(long hotKeyCacheStatsCallbackIntervalSeconds) {
        this.hotKeyCacheStatsCallbackIntervalSeconds = hotKeyCacheStatsCallbackIntervalSeconds;
    }

    public HotKeyCacheStatsCallback getHotKeyCacheStatsCallback() {
        return hotKeyCacheStatsCallback;
    }

    public void setHotKeyCacheStatsCallback(HotKeyCacheStatsCallback hotKeyCacheStatsCallback) {
        this.hotKeyCacheStatsCallback = hotKeyCacheStatsCallback;
    }

    public long getHotKeyCacheExpireMillis() {
        return hotKeyCacheExpireMillis;
    }

    public void setHotKeyCacheExpireMillis(long hotKeyCacheExpireMillis) {
        this.hotKeyCacheExpireMillis = hotKeyCacheExpireMillis;
    }

    public long getHotKeyCacheMaxCapacity() {
        return hotKeyCacheMaxCapacity;
    }

    public void setHotKeyCacheMaxCapacity(long hotKeyCacheMaxCapacity) {
        this.hotKeyCacheMaxCapacity = hotKeyCacheMaxCapacity;
    }

    public long getHotKeyCacheCounterCheckMillis() {
        return hotKeyCacheCounterCheckMillis;
    }

    public void setHotKeyCacheCounterCheckMillis(long hotKeyCacheCounterCheckMillis) {
        this.hotKeyCacheCounterCheckMillis = hotKeyCacheCounterCheckMillis;
    }

    public long getHotKeyCacheCounterMaxCapacity() {
        return hotKeyCacheCounterMaxCapacity;
    }

    public void setHotKeyCacheCounterMaxCapacity(long hotKeyCacheCounterMaxCapacity) {
        this.hotKeyCacheCounterMaxCapacity = hotKeyCacheCounterMaxCapacity;
    }

    public long getHotKeyCacheCounterCheckThreshold() {
        return hotKeyCacheCounterCheckThreshold;
    }

    public void setHotKeyCacheCounterCheckThreshold(long hotKeyCacheCounterCheckThreshold) {
        this.hotKeyCacheCounterCheckThreshold = hotKeyCacheCounterCheckThreshold;
    }

    public HotKeyCacheKeyChecker getHotKeyCacheKeyChecker() {
        return hotKeyCacheKeyChecker;
    }

    public void setHotKeyCacheKeyChecker(HotKeyCacheKeyChecker hotKeyCacheKeyChecker) {
        this.hotKeyCacheKeyChecker = hotKeyCacheKeyChecker;
    }

    public boolean isHotKeyCacheNeedCacheNull() {
        return hotKeyCacheNeedCacheNull;
    }

    public void setHotKeyCacheNeedCacheNull(boolean hotKeyCacheNeedCacheNull) {
        this.hotKeyCacheNeedCacheNull = hotKeyCacheNeedCacheNull;
    }
}
