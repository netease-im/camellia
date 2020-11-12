package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/11/8
 */
public class HotKeyCacheManager {
    private final ConcurrentHashMap<String, HotKeyCache> map = new ConcurrentHashMap<>();
    private final HotKeyCache hotKeyCache;

    private final CommandHotKeyCacheConfig commandHotKeyCacheConfig;

    public HotKeyCacheManager(CommandHotKeyCacheConfig commandHotKeyCacheConfig) {
        this.commandHotKeyCacheConfig = commandHotKeyCacheConfig;
        this.hotKeyCache = new HotKeyCache(new CommandContext(null, null), commandHotKeyCacheConfig);
    }

    public HotKeyCache get(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return hotKeyCache;
        } else {
            String key = bid + "|" + bgroup;
            HotKeyCache hotKeyCache = map.get(key);
            if (hotKeyCache == null) {
                synchronized (HotKeyCacheManager.class) {
                    hotKeyCache = map.get(key);
                    if (hotKeyCache == null) {
                        hotKeyCache = new HotKeyCache(new CommandContext(bid, bgroup),commandHotKeyCacheConfig);
                        map.put(key, hotKeyCache);
                    }
                }
            }
            return hotKeyCache;
        }
    }
}
