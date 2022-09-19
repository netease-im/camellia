package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;

import java.util.List;

/**
 *
 * Created by caojiajun on 2021/4/25
 */
public class DummyHotKeyMonitorCallback implements HotKeyMonitorCallback {

    @Override
    public void callback(IdentityInfo identityInfo, List<HotKeyInfo> hotKeys, long checkMillis, long checkThreshold) {

    }
}
