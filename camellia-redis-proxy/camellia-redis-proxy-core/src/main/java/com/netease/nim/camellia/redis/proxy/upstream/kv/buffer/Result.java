package com.netease.nim.camellia.redis.proxy.upstream.kv.buffer;

/**
 * Created by caojiajun on 2024/5/22
 */
public interface Result {

    boolean isKvWriteDelayEnable();
    void kvWriteDone();
}
