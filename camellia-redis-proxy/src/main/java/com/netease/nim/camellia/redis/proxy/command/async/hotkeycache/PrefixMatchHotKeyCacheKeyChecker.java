package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.Set;

/**
 *
 * Created by caojiajun on 2021/1/5
 */
public class PrefixMatchHotKeyCacheKeyChecker implements HotKeyCacheKeyChecker {

    @Override
    public boolean needCache(CommandContext commandContext, byte[] key) {
        Long bid = commandContext.getBid();
        String bgroup = commandContext.getBgroup();
        Set<String> set = ProxyDynamicConf.hotKeyCacheKeyPrefix(bid, bgroup);
        if (set.isEmpty()) return false;
        String keyStr = Utils.bytesToString(key);
        for (String prefix : set) {
            if (keyStr.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
