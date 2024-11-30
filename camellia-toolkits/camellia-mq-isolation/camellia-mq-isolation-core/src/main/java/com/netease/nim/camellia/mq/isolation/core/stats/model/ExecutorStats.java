package com.netease.nim.camellia.mq.isolation.core.stats.model;

/**
 * Created by caojiajun on 2024/2/23
 */
public class ExecutorStats {
    private String name;
    private int threads;
    private int currentThreads;
    private int activeThreads;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getCurrentThreads() {
        return currentThreads;
    }

    public void setCurrentThreads(int currentThreads) {
        this.currentThreads = currentThreads;
    }

    public int getActiveThreads() {
        return activeThreads;
    }

    public void setActiveThreads(int activeThreads) {
        this.activeThreads = activeThreads;
    }
}
