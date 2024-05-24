package com.netease.nim.camellia.redis.proxy.upstream.kv.buffer;

/**
 * Created by caojiajun on 2024/5/22
 */
public class NoOpResult implements Result {

    public static final NoOpResult INSTANCE = new NoOpResult();

    private NoOpResult() {
    }

    @Override
    public final boolean isKvWriteDelayEnable() {
        return false;
    }

    @Override
    public final void kvWriteDone() {

    }
}
