package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public interface HotKeyCacheKeyChecker {

    boolean needCache(byte[] key);
}
