package com.netease.nim.camellia.redis.proxy.hotkey.common;

import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;
import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;

public interface ProxyHotKeyServerDiscoveryFactory {
    HotKeyServerDiscovery getDiscovery();
}
