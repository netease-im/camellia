package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.flush;

/**
 * Created by caojiajun on 2025/1/7
 */
public class FlushThread extends Thread {

    public FlushThread() {
    }

    public FlushThread(Runnable task) {
        super(task);
    }

    public FlushThread(ThreadGroup group, Runnable task) {
        super(group, task);
    }

    public FlushThread(String name) {
        super(name);
    }

    public FlushThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public FlushThread(Runnable task, String name) {
        super(task, name);
    }

    public FlushThread(ThreadGroup group, Runnable task, String name) {
        super(group, task, name);
    }

    public FlushThread(ThreadGroup group, Runnable task, String name, long stackSize) {
        super(group, task, name, stackSize);
    }

    public FlushThread(ThreadGroup group, Runnable task, String name, long stackSize, boolean inheritInheritableThreadLocals) {
        super(group, task, name, stackSize, inheritInheritableThreadLocals);
    }
}
