package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.core.util.LockMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/11/8
 */
public class HotKeyCacheManager {
    private final ConcurrentHashMap<String, HotKeyCache> map = new ConcurrentHashMap<>();
    private final LockMap lockMap = new LockMap();
    private final HotKeyCache hotKeyCache;

    private final HotKeyCacheConfig hotKeyCacheConfig;

    public HotKeyCacheManager(HotKeyCacheConfig hotKeyCacheConfig) {
        this.hotKeyCacheConfig = hotKeyCacheConfig;
        this.hotKeyCache = new HotKeyCache(new IdentityInfo(null, null), hotKeyCacheConfig);
    }

    public HotKeyCache get(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return hotKeyCache;
        } else {
            String key = bid + "|" + bgroup;
            HotKeyCache hotKeyCache = map.get(key);
            if (hotKeyCache == null) {
                synchronized (lockMap.getLockObj(key)) {
                    hotKeyCache = map.get(key);
                    if (hotKeyCache == null) {
                        hotKeyCache = new HotKeyCache(new IdentityInfo(bid, bgroup), hotKeyCacheConfig);
                        map.put(key, hotKeyCache);
                    }
                }
            }
            return hotKeyCache;
        }
    }
}
