package com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caojiajun on 2025/1/7
 */
public class FlushThread extends Thread {

    private static final AtomicInteger idGen = new AtomicInteger(0);

    private final int id;

    public FlushThread(ThreadGroup group, Runnable task, String name, long stackSize) {
        super(group, task, name, stackSize);
        id = idGen.getAndIncrement();
    }

    public int getThreadId() {
        return id;
    }
}
