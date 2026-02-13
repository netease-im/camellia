package com.netease.nim.camellia.redis.proxy.hotkeyserver.cache;

import com.netease.nim.camellia.hot.key.sdk.ICamelliaHotKeyCacheSdk;
import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.LockMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 工厂模式，创建单例{@link HotKeyCache}
 */
public class HotKeyCacheManager {
    /**
     * 如果不是多租户，就只有一个{@link HotKeyCache}
     */
    private final HotKeyCache hotKeyCache;

    public ConcurrentHashMap<String, HotKeyCache> hotKeyCacheContainer = new ConcurrentHashMap<>();

    private final LockMap lockMap = new LockMap();

    private final ICamelliaHotKeyCacheSdk hotKeyCacheSdk;
    private final HotKeyCacheKeyChecker keyChecker;

    public HotKeyCacheManager(ICamelliaHotKeyCacheSdk hotKeyCacheSdk, HotKeyCacheKeyChecker keyChecker) {
        this.hotKeyCacheSdk = hotKeyCacheSdk;
        this.keyChecker = keyChecker;
        hotKeyCache = new HotKeyCache(new IdentityInfo(null, null), hotKeyCacheSdk, keyChecker);
    }

    /**
     * DCL
     */
    public HotKeyCache getHotKeyCache(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return hotKeyCache;
        } else {
            String key = Utils.getCacheKey(bid, bgroup);
            HotKeyCache hotKeyCache = hotKeyCacheContainer.get(key);
            if (hotKeyCache == null) {
                synchronized (lockMap.getLockObj(key)) {
                    hotKeyCache = hotKeyCacheContainer.get(key);
                    if (hotKeyCache == null) {
                        hotKeyCache = new HotKeyCache(new IdentityInfo(bid, bgroup), hotKeyCacheSdk, keyChecker);
                        hotKeyCacheContainer.put(key, hotKeyCache);
                    }
                }
            }
            return hotKeyCache;
        }
    }
}
