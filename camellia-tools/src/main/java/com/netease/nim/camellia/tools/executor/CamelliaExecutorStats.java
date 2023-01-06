package com.netease.nim.camellia.tools.executor;

/**
 * Created by caojiajun on 2023/1/4
 */
public class CamelliaExecutorStats {

    private int thread;
    private int activeThread;
    private long pendingTask;
    private long completedTaskCount;

    public int getThread() {
        return thread;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public int getActiveThread() {
        return activeThread;
    }

    public void setActiveThread(int activeThread) {
        this.activeThread = activeThread;
    }

    public long getPendingTask() {
        return pendingTask;
    }

    public void setPendingTask(long pendingTask) {
        this.pendingTask = pendingTask;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    public void setCompletedTaskCount(long completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }
}
