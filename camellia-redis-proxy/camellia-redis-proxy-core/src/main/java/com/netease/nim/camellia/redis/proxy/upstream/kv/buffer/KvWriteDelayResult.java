package com.netease.nim.camellia.redis.proxy.upstream.kv.buffer;

/**
 * Created by caojiajun on 2024/5/22
 */
public class KvWriteDelayResult implements Result {

    private final Runnable runnable;

    public KvWriteDelayResult(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public boolean isKvWriteDelayEnable() {
        return true;
    }

    @Override
    public void kvWriteDone() {
        runnable.run();
    }
}
