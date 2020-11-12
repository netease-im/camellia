package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class DummyHotKeyCacheKeyChecker implements HotKeyCacheKeyChecker {

    @Override
    public boolean needCache(CommandContext commandContext, byte[] key) {
        return true;
    }
}
