package com.netease.nim.camellia.redis.proxy.hotkey.mointor.plugin;

import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;

public class HotKeyMonitorConfig {
    private HotKeyServerDiscovery discovery;

    public HotKeyServerDiscovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(HotKeyServerDiscovery discovery) {
        this.discovery = discovery;
    }
}
