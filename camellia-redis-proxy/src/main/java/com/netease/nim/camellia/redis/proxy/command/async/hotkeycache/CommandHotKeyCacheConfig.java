package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.conf.Constants;


/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class CommandHotKeyCacheConfig {

    private long cacheExpireMillis = Constants.Server.hotKeyCacheExpireMillis;
    private long cacheMaxCapacity = Constants.Server.hotKeyCacheMaxCapacity;

    private long counterCheckMillis = Constants.Server.hotKeyCacheCounterCheckMillis;
    private long counterMaxCapacity = Constants.Server.hotKeyCacheCounterMaxCapacity;
    private long counterCheckThreshold = Constants.Server.hotKeyCacheCounterCheckThreshold;
    private boolean needCacheNull = Constants.Server.hotKeyCacheNeedCacheNull;

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

    public long getCacheExpireMillis() {
        return cacheExpireMillis;
    }

    public void setCacheExpireMillis(long cacheExpireMillis) {
        this.cacheExpireMillis = cacheExpireMillis;
    }

    public long getCacheMaxCapacity() {
        return cacheMaxCapacity;
    }

    public void setCacheMaxCapacity(long cacheMaxCapacity) {
        this.cacheMaxCapacity = cacheMaxCapacity;
    }

    public long getCounterCheckMillis() {
        return counterCheckMillis;
    }

    public void setCounterCheckMillis(long counterCheckMillis) {
        this.counterCheckMillis = counterCheckMillis;
    }

    public long getCounterMaxCapacity() {
        return counterMaxCapacity;
    }

    public void setCounterMaxCapacity(long counterMaxCapacity) {
        this.counterMaxCapacity = counterMaxCapacity;
    }

    public long getCounterCheckThreshold() {
        return counterCheckThreshold;
    }

    public void setCounterCheckThreshold(long counterCheckThreshold) {
        this.counterCheckThreshold = counterCheckThreshold;
    }

    public HotKeyCacheKeyChecker getHotKeyCacheKeyChecker() {
        return hotKeyCacheKeyChecker;
    }

    public void setHotKeyCacheKeyChecker(HotKeyCacheKeyChecker hotKeyCacheKeyChecker) {
        this.hotKeyCacheKeyChecker = hotKeyCacheKeyChecker;
    }

    public boolean isNeedCacheNull() {
        return needCacheNull;
    }

    public void setNeedCacheNull(boolean needCacheNull) {
        this.needCacheNull = needCacheNull;
    }
}
