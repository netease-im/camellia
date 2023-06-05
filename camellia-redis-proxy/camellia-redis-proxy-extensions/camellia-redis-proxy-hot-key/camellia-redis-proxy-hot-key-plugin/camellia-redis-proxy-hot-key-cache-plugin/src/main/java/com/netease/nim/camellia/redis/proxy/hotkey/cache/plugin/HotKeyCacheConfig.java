package com.netease.nim.camellia.redis.proxy.hotkey.cache.plugin;

import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotKeyCacheKeyChecker;

public class HotKeyCacheConfig {
    private HotKeyServerDiscovery discovery;
    private HotKeyCacheKeyChecker hotKeyCacheKeyChecker;

    public HotKeyServerDiscovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(HotKeyServerDiscovery discovery) {
        this.discovery = discovery;
    }

    public HotKeyCacheKeyChecker getHotKeyCacheKeyChecker() {
        return hotKeyCacheKeyChecker;
    }

    public void setHotKeyCacheKeyChecker(HotKeyCacheKeyChecker hotKeyCacheKeyChecker) {
        this.hotKeyCacheKeyChecker = hotKeyCacheKeyChecker;
    }
}
