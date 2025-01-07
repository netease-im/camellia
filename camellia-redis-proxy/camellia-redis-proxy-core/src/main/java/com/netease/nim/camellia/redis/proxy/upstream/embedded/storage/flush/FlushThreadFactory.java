package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.flush;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caojiajun on 2025/1/7
 */
public class FlushThreadFactory implements ThreadFactory {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean daemon;

    public FlushThreadFactory(String name, boolean daemon) {
        this.group = Thread.currentThread().getThreadGroup();
        this.namePrefix = name + "-pool-" + poolNumber.getAndIncrement() + "-thread-";
        this.daemon = daemon;
    }

    public Thread newThread(Runnable r) {
        Thread t = new FlushThread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (daemon) {
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
        } else {
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
