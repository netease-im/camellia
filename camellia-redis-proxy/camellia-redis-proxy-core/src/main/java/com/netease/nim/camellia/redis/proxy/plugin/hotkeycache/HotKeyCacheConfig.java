package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;



/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class HotKeyCacheConfig {

    /**
     * Check if the key needs to be cached
     */
    private HotKeyCacheKeyChecker hotKeyCacheKeyChecker;
    private HotKeyCacheStatsCallback hotKeyCacheStatsCallback;

    public HotKeyCacheKeyChecker getHotKeyCacheKeyChecker() {
        return hotKeyCacheKeyChecker;
    }

    public void setHotKeyCacheKeyChecker(HotKeyCacheKeyChecker hotKeyCacheKeyChecker) {
        this.hotKeyCacheKeyChecker = hotKeyCacheKeyChecker;
    }

    public HotKeyCacheStatsCallback getHotKeyCacheStatsCallback() {
        return hotKeyCacheStatsCallback;
    }

    public void setHotKeyCacheStatsCallback(HotKeyCacheStatsCallback hotKeyCacheStatsCallback) {
        this.hotKeyCacheStatsCallback = hotKeyCacheStatsCallback;
    }
}
