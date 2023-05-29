package com.netease.nim.camellia.redis.proxy.plugin.hotkeycacheserver;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotKeyCache;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotKeyCacheConfig;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.LockMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 工厂模式，创建单例{@link HotKeyCacheBasedServer}
 */
public class HotKeyCacheBasedServerManager {
    private static volatile HotKeyCacheBasedServer hotKeyCacheInstance;

    public HotKeyCacheBasedServerManager() {
    }

    public static HotKeyCacheBasedServer getHotKeyCache(Long bid, String bgroup) {
        if (hotKeyCacheInstance == null) {
            synchronized (HotKeyCacheBasedServerManager.class) {
                if (hotKeyCacheInstance == null) {
                    hotKeyCacheInstance = new HotKeyCacheBasedServer(new IdentityInfo(bid, bgroup));
                }
            }
        }
        return hotKeyCacheInstance;
    }
}
