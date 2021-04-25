package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;

/**
 *
 * Created by caojiajun on 2021/4/25
 */
public class DummyHotKeyCacheStatsCallback implements HotKeyCacheStatsCallback {

    @Override
    public void callback(CommandContext commandContext, HotKeyCacheStats hotKeyCacheStats,
                         CommandHotKeyCacheConfig commandHotKeyCacheConfig) {
    }
}
