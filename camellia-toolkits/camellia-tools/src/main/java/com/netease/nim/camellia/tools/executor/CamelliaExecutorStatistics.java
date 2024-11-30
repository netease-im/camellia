package com.netease.nim.camellia.tools.executor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/1/4
 */
public class CamelliaExecutorStatistics {

    private List<ExecutorStats> executorStatsList = new ArrayList<>();
    private List<DynamicIsolationExecutorStats> dynamicIsolationExecutorStatsList = new ArrayList<>();

    public List<ExecutorStats> getExecutorStatsList() {
        return executorStatsList;
    }

    public void setExecutorStatsList(List<ExecutorStats> executorStatsList) {
        this.executorStatsList = executorStatsList;
    }

    public List<DynamicIsolationExecutorStats> getDynamicIsolationExecutorStatsList() {
        return dynamicIsolationExecutorStatsList;
    }

    public void setDynamicIsolationExecutorStatsList(List<DynamicIsolationExecutorStats> dynamicIsolationExecutorStatsList) {
        this.dynamicIsolationExecutorStatsList = dynamicIsolationExecutorStatsList;
    }

    public static class DynamicIsolationExecutorStats {
        private String name;
        private Stats fastExecutorStats;
        private Stats fastBackupExecutorStats;
        private Stats slowExecutorStats;
        private Stats slowBackupExecutorStats;
        private Stats whiteListExecutorStats;
        private Stats isolationExecutorStats;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Stats getFastExecutorStats() {
            return fastExecutorStats;
        }

        public void setFastExecutorStats(Stats fastExecutorStats) {
            this.fastExecutorStats = fastExecutorStats;
        }

        public Stats getFastBackupExecutorStats() {
            return fastBackupExecutorStats;
        }

        public void setFastBackupExecutorStats(Stats fastBackupExecutorStats) {
            this.fastBackupExecutorStats = fastBackupExecutorStats;
        }

        public Stats getSlowExecutorStats() {
            return slowExecutorStats;
        }

        public void setSlowExecutorStats(Stats slowExecutorStats) {
            this.slowExecutorStats = slowExecutorStats;
        }

        public Stats getSlowBackupExecutorStats() {
            return slowBackupExecutorStats;
        }

        public void setSlowBackupExecutorStats(Stats slowBackupExecutorStats) {
            this.slowBackupExecutorStats = slowBackupExecutorStats;
        }

        public Stats getWhiteListExecutorStats() {
            return whiteListExecutorStats;
        }

        public void setWhiteListExecutorStats(Stats whiteListExecutorStats) {
            this.whiteListExecutorStats = whiteListExecutorStats;
        }

        public Stats getIsolationExecutorStats() {
            return isolationExecutorStats;
        }

        public void setIsolationExecutorStats(Stats isolationExecutorStats) {
            this.isolationExecutorStats = isolationExecutorStats;
        }
    }

    public static enum ExecutorType {
        ThreadPoolExecutor,
        CamelliaDynamicExecutor,
        CamelliaHashedExecutor,
        CamelliaDynamicIsolationExecutor,
        CamelliaLinearInitializationExecutor,
        ;
    }

    public static class ExecutorStats {
        private String name;
        private ExecutorType executorType;
        private Stats stats;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ExecutorType getExecutorType() {
            return executorType;
        }

        public void setExecutorType(ExecutorType executorType) {
            this.executorType = executorType;
        }

        public Stats getStats() {
            return stats;
        }

        public void setStats(Stats stats) {
            this.stats = stats;
        }
    }

    public static class Stats {
        private int thread;
        private int activeThread;
        private long pendingTask;
        private long taskCount;

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

        public long getTaskCount() {
            return taskCount;
        }

        public void setTaskCount(long taskCount) {
            this.taskCount = taskCount;
        }

    }
}
