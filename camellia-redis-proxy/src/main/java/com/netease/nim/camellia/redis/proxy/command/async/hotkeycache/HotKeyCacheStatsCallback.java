package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public interface HotKeyCacheStatsCallback {

    /**
     * callback the hot key cache stats
     * @param commandContext commandContext, such as bid/bgroup
     * @param hotKeyCacheStats hot key cache stats
     * @param commandHotKeyCacheConfig config of hot key cache
     */
    void callback(CommandContext commandContext, HotKeyCacheStats hotKeyCacheStats, CommandHotKeyCacheConfig commandHotKeyCacheConfig);
}
