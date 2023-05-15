package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.alibaba.fastjson.JSONArray;
import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对key进行前缀匹配
 * Created by caojiajun on 2021/1/5
 */
public class PrefixMatchHotKeyCacheKeyChecker implements HotKeyCacheKeyChecker {

    private final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    public PrefixMatchHotKeyCacheKeyChecker() {
        ProxyDynamicConf.registerCallback(cache::clear);
    }

    @Override
    public boolean needCache(IdentityInfo identityInfo, byte[] key) {
        try {
            Long bid = identityInfo.getBid();
            String bgroup = identityInfo.getBgroup();
            String cacheKey = Utils.getCacheKey(bid, bgroup);
            // 这里不用DCL是因为不用保证每次获取是同一个对象，也就是说不用做成单例
            Set<String> set = cache.get(cacheKey);
            if (set == null) {
                set = hotKeyCacheKeyPrefix(bid, bgroup);
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

    private Set<String> hotKeyCacheKeyPrefix(Long bid, String bgroup) {
        String conf = ProxyDynamicConf.getString("hot.key.cache.key.prefix", bid, bgroup, "");
        if (conf == null || conf.trim().length() == 0) {
            return new HashSet<>();
        }
        Set<String> set = new HashSet<>();
        try {
            JSONArray array = JSONArray.parseArray(conf);
            for (Object o : array) {
                set.add(String.valueOf(o));
            }
        } catch (Exception ignore) {
        }
        return set;
    }
}
