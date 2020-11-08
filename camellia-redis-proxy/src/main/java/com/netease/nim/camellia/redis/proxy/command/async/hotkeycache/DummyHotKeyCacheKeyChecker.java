package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class DummyHotKeyCacheKeyChecker implements HotKeyCacheKeyChecker {

    @Override
    public boolean needCache(byte[] key) {
        return true;
    }
}
