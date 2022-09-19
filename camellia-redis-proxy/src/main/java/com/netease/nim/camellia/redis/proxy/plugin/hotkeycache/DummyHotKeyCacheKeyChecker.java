package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class DummyHotKeyCacheKeyChecker implements HotKeyCacheKeyChecker {

    @Override
    public boolean needCache(IdentityInfo identityInfo, byte[] key) {
        return true;
    }
}
