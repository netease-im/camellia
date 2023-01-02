package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public interface HotKeyCacheStatsCallback {

    /**
     * callback the hot key cache stats
     */
    void callback(IdentityInfo identityInfo, HotKeyCacheInfo hotKeyCacheStats, long checkMillis, long checkThreshold);
}
