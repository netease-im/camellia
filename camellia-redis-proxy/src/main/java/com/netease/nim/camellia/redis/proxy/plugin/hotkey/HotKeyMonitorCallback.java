package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public interface HotKeyMonitorCallback {

    void callback(IdentityInfo identityInfo, List<HotKeyInfo> hotKeys, long checkMillis, long checkThreshold);
}
