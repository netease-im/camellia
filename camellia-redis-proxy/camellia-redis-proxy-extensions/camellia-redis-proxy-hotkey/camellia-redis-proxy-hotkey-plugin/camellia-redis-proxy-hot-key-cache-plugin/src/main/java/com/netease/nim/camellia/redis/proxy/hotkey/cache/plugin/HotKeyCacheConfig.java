package com.netease.nim.camellia.redis.proxy.hotkey.cache.plugin;

import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;

public class HotKeyCacheConfig {
    private HotKeyServerDiscovery discovery;

    public HotKeyServerDiscovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(HotKeyServerDiscovery discovery) {
        this.discovery = discovery;
    }
}
