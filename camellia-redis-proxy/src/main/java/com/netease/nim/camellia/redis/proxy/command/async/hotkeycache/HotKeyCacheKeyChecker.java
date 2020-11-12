package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public interface HotKeyCacheKeyChecker {

    /**
     * check if the key need be cached if hot
     * @param commandContext commandContext, such as bid/bgroup
     * @param key key
     * @return true/false
     */
    boolean needCache(CommandContext commandContext, byte[] key);
}
