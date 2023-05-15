package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.LockMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 工厂模式
 * Created by caojiajun on 2020/11/8
 */
public class HotKeyCacheManager {
    /**
     * 针对不同租户缓存的map，key: bid + "|" + bgroup, value: {@link HotKeyCache}
     */
    private final ConcurrentHashMap<String, HotKeyCache> map = new ConcurrentHashMap<>();
    /**
     * 锁map，锁粒度是不同的key；为了对 {@link HotKeyCacheManager#map} 并发加载的时候不同key之间互不影响
     */
    private final LockMap lockMap = new LockMap();
    /**
     * 如果不是多租户，就只有一个{@link HotKeyCache}
     */
    private final HotKeyCache hotKeyCache;

    /**
     * 配置
     */
    private final HotKeyCacheConfig hotKeyCacheConfig;

    public HotKeyCacheManager(HotKeyCacheConfig hotKeyCacheConfig) {
        this.hotKeyCacheConfig = hotKeyCacheConfig;
        this.hotKeyCache = new HotKeyCache(new IdentityInfo(null, null), hotKeyCacheConfig);
    }

    /**
     * 懒加载，保证单例也就是一个key对应唯一一个cache，用DCL双重检测去优化并发。
     *
     * @param bid    多租户的id
     * @param bgroup 多租户的组
     * @return {@link HotKeyCache} 热keyCache本地缓存
     */
    public HotKeyCache get(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return hotKeyCache;
        } else {
            String key = Utils.getCacheKey(bid, bgroup);
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
