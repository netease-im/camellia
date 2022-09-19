package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public interface HotKeyCacheKeyChecker {

    /**
     * check if the key need be cached if hot
     * @param identityInfo IdentityInfo, such as bid/bgroup
     * @param key key
     * @return true/false
     */
    boolean needCache(IdentityInfo identityInfo, byte[] key);
}
