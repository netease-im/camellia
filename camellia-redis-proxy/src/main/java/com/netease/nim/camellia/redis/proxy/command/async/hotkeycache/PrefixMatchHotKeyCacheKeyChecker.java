package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2021/1/5
 */
public class PrefixMatchHotKeyCacheKeyChecker implements HotKeyCacheKeyChecker {

    private final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    public PrefixMatchHotKeyCacheKeyChecker() {
        ProxyDynamicConf.registerCallback(cache::clear);
    }

    @Override
    public boolean needCache(CommandContext commandContext, byte[] key) {
        try {
            Long bid = commandContext.getBid();
            String bgroup = commandContext.getBgroup();
            String cacheKey = bid + "|" + bgroup;
            Set<String> set = cache.get(cacheKey);
            if (set == null) {
                set = ProxyDynamicConf.hotKeyCacheKeyPrefix(bid, bgroup);
                cache.put(cacheKey, set);
            }
            if (set.isEmpty()) return false;
            String keyStr = Utils.bytesToString(key);
            for (String prefix : set) {
                if (keyStr.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            ErrorLogCollector.collect(PrefixMatchHotKeyCacheKeyChecker.class, "check key prefix error", e);
            return false;
        }
    }
}
