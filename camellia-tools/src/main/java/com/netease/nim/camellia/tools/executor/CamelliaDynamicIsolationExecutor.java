package com.netease.nim.camellia.tools.executor;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个可以根据isolationKey自动选择不同线程池的执行器
 * 内部分为四个线程池：
 * 1）fastExecutor，执行耗时较短的任务
 * 2）fastBackUpExecutor，耗时较短，有突发流量，则先进入fastBackUpExecutor
 * 3）slowExecutor，执行耗时较长的任务
 * 4）slowBackupExecutor，耗时较长，有突发流量，则先进入slowBackupExecutor
 * 5）whiteListExecutor，白名单isolationKey在这里执行，不关心统计数据
 * 6）isolationExecutor，隔离线程池，应对突发流量或者特别慢的任务
 *
 * 规则：
 * 1）默认fastExecutor
 * 2）如果统计为慢，则进入slowExecutor
 * 3）如果因为fastExecutor繁忙而提交失败，则进入fastBackUpExecutor，如果仍然繁忙，则进入isolationExecutor
 * 4）如果因为slowExecutor繁忙而提交失败，则进入slowBackupExecutor，如果仍然繁忙，则进入isolationExecutor
 * 5）如果占用线程超过线程池指定比例（默认0.5），则直接进入isolationExecutor
 * 6）在白名单列表里的isolationKey，直接在whiteListExecutor中执行，如果繁忙，则进入isolationExecutor
 * 7）最终所有任务都会把isolationExecutor作为兜底，isolationExecutor处理不了的情况下的拒绝策略可以自定义
 *
 * Created by caojiajun on 2023/1/3
 */
public class CamelliaDynamicIsolationExecutor implements CamelliaExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDynamicIsolationExecutor.class);

    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
            new CamelliaThreadFactory("camellia-dynamic-isolation-executor-schedule"));
    private static final ScheduledExecutorService statsScheduleExecutor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory("camellia-dynamic-isolation-executor-stats-schedule"));

    private final CamelliaDynamicIsolationExecutorConfig config;
    private final String name;
    private final DynamicValueGetter<Integer> poolSize;
    private final ThreadPoolExecutor fastExecutor;
    private final ThreadPoolExecutor fastBackUpExecutor;
    private final ThreadPoolExecutor slowExecutor;
    private final ThreadPoolExecutor slowBackUpExecutor;
    private final ThreadPoolExecutor isolationExecutor;
    private final ThreadPoolExecutor whiteListExecutor;

    private int currentPoolSize;
    private double isolationThresholdPercentage;
    private Set<String> currentWhiteListIsolationKeys;

    private final ConcurrentLinkedHashMap<String, Semaphore> semaphoreMap;
    private final ConcurrentLinkedHashMap<String, Stats> statsMap;

    private static final ThreadLocal<Type> currentType = new ThreadLocal<>();

    public CamelliaDynamicIsolationExecutor(String name, int poolSize) {
        this(new CamelliaDynamicIsolationExecutorConfig(name, () -> poolSize));
    }

    public CamelliaDynamicIsolationExecutor(String name, int poolSize, int queueSize) {
        this(new CamelliaDynamicIsolationExecutorConfig(name, () -> poolSize, () -> queueSize));
    }

    public CamelliaDynamicIsolationExecutor(CamelliaDynamicIsolationExecutorConfig config) {
        this.config = config;
        this.name = CamelliaExecutorMonitor.genExecutorName(config.getName());
        this.poolSize = config.getPoolSize();
        this.currentPoolSize = poolSize.get();
        this.isolationThresholdPercentage = config.getIsolationThresholdPercentage().get();
        this.fastExecutor = new ThreadPoolExecutor(currentPoolSize, currentPoolSize, 0, TimeUnit.SECONDS,
                new DynamicCapacityLinkedBlockingQueue<>(config.getQueueSize()),
                new CamelliaThreadFactory("[camellia-dynamic-isolation-executor][fast][" + name + "]"), new ThreadPoolExecutor.AbortPolicy());
        this.fastBackUpExecutor = new ThreadPoolExecutor(currentPoolSize, currentPoolSize, 0, TimeUnit.SECONDS,
                new DynamicCapacityLinkedBlockingQueue<>(config.getQueueSize()),
                new CamelliaThreadFactory("[camellia-dynamic-isolation-executor][fast-backup][" + name + "]"), new ThreadPoolExecutor.AbortPolicy());
        this.slowExecutor = new ThreadPoolExecutor(currentPoolSize, currentPoolSize, 0, TimeUnit.SECONDS,
                new DynamicCapacityLinkedBlockingQueue<>(config.getQueueSize()),
                new CamelliaThreadFactory("[camellia-dynamic-isolation-executor][slow][" + name + "]"), new ThreadPoolExecutor.AbortPolicy());
        this.slowBackUpExecutor = new ThreadPoolExecutor(currentPoolSize, currentPoolSize, 0, TimeUnit.SECONDS,
                new DynamicCapacityLinkedBlockingQueue<>(config.getQueueSize()),
                new CamelliaThreadFactory("[camellia-dynamic-isolation-executor][slow-backup][" + name + "]"), new ThreadPoolExecutor.AbortPolicy());
        this.whiteListExecutor = new ThreadPoolExecutor(currentPoolSize, currentPoolSize, 0, TimeUnit.SECONDS,
                new DynamicCapacityLinkedBlockingQueue<>(config.getQueueSize()),
                new CamelliaThreadFactory("[camellia-dynamic-isolation-executor][white-list][" + name + "]"), new ThreadPoolExecutor.AbortPolicy());
        this.isolationExecutor = new ThreadPoolExecutor(currentPoolSize, currentPoolSize, 0, TimeUnit.SECONDS,
                new DynamicCapacityLinkedBlockingQueue<>(config.getQueueSize()),
                new CamelliaThreadFactory("[camellia-dynamic-isolation-executor][isolation][" + name + "]"), new ThreadPoolExecutor.AbortPolicy());

        this.currentWhiteListIsolationKeys = new HashSet<>(config.getWhiteListIsolationKeys().get());

        this.semaphoreMap = new ConcurrentLinkedHashMap.Builder<String, Semaphore>()
                .initialCapacity(config.getMaxIsolationKeyCount())
                .maximumWeightedCapacity(config.getMaxIsolationKeyCount())
                .build();
        this.statsMap = new ConcurrentLinkedHashMap.Builder<String, Stats>()
                .initialCapacity(config.getMaxIsolationKeyCount())
                .maximumWeightedCapacity(config.getMaxIsolationKeyCount())
                .build();

        scheduledExecutor.scheduleAtFixedRate(this::refresh, 10, 10, TimeUnit.SECONDS);
        statsScheduleExecutor.scheduleAtFixedRate(this::trySlideStatsWindow,
                config.getStatisticSlidingWindowTime(), config.getStatisticSlidingWindowTime(), TimeUnit.MILLISECONDS);

        CamelliaExecutorMonitor.register(this);
    }

    private static final Fallback dummyFallback = (isolationKey, runnable) -> {};

    public static interface Fallback {
        void fallback(String isolationKey, Runnable runnable);
    }

    /**
     * 提交一个任务
     * @param isolationKey 隔离key
     * @param runnable 任务
     */
    public void submit(String isolationKey, Runnable runnable) {
        submit(isolationKey, runnable, dummyFallback);
    }

    /**
     * 提交一个任务
     * @param isolationKey 隔离key
     * @param runnable 任务
     * @param fallback 任务未执行时的fallback回调
     */
    public void submit(String isolationKey, Runnable runnable, Fallback fallback) {
        ExecutorChooseResult result = chooseExecutor(isolationKey);
        try {
            doTask(isolationKey, result.executor, runnable, result.type, fallback);
        } catch (RejectedExecutionException e) {
            if (result.type == Type.FAST) {
                try {
                    doTask(isolationKey, fastBackUpExecutor, runnable, Type.FAST_BACKUP, fallback);
                } catch (RejectedExecutionException ex) {
                    try {
                        doTask(isolationKey, isolationExecutor, runnable, Type.ISOLATION, fallback);
                    } catch (RejectedExecutionException exc) {
                        fallback.fallback(isolationKey, runnable);
                    }
                }
                return;
            }
            if (result.type == Type.SLOW) {
                try {
                    doTask(isolationKey, slowBackUpExecutor, runnable, Type.SLOW_BACKUP, fallback);
                } catch (RejectedExecutionException ex) {
                    try {
                        doTask(isolationKey, isolationExecutor, runnable, Type.ISOLATION, fallback);
                    } catch (RejectedExecutionException exc) {
                        fallback.fallback(isolationKey, runnable);
                    }
                }
                return;
            }
            if (result.type != Type.ISOLATION) {
                try {
                    doTask(isolationKey, isolationExecutor, runnable, Type.ISOLATION, fallback);
                } catch (RejectedExecutionException exc) {
                    fallback.fallback(isolationKey, runnable);
                }
            } else {
                fallback.fallback(isolationKey, runnable);
            }
        }
    }

    private void doTask(String isolationKey, ThreadPoolExecutor executor, Runnable runnable, Type type, Fallback fallback) {
        executor.submit(() -> {
            Semaphore semaphore = null;
            if (type != Type.ISOLATION) {
                semaphore = getSemaphore(isolationKey);
                if (!semaphore.tryAcquire()) {
                    try {
                        isolationExecutor.submit(() -> {
                            currentType.set(Type.ISOLATION);
                            long startMs = System.currentTimeMillis();
                            try {
                                runnable.run();
                            } finally {
                                afterExecute(null, startMs, isolationKey);
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        fallback.fallback(isolationKey, runnable);
                    }
                    return;
                }
            }
            currentType.set(type);
            long startMs = System.currentTimeMillis();
            try {
                runnable.run();
            } finally {
                afterExecute(semaphore, startMs, isolationKey);
            }
        });
    }

    /**
     * 获取当前所在的线程池类型
     */
    public static Type getCurrentExecutorType() {
        return currentType.get();
    }

    public CamelliaDynamicIsolationExecutorConfig getConfig() {
        return config;
    }

    private void refresh() {
        try {
            if (poolSize.get() != currentPoolSize) {
                int currentPoolSize = poolSize.get();
                fastExecutor.setCorePoolSize(currentPoolSize);
                fastExecutor.setMaximumPoolSize(currentPoolSize);
                fastBackUpExecutor.setCorePoolSize(currentPoolSize);
                fastBackUpExecutor.setMaximumPoolSize(currentPoolSize);
                slowExecutor.setCorePoolSize(currentPoolSize);
                slowExecutor.setMaximumPoolSize(currentPoolSize);
                slowBackUpExecutor.setCorePoolSize(currentPoolSize);
                slowBackUpExecutor.setMaximumPoolSize(currentPoolSize);
                isolationExecutor.setCorePoolSize(currentPoolSize);
                isolationExecutor.setMaximumPoolSize(currentPoolSize);
                logger.info("dynamic-isolation-executor, poolSize update, name = {}, {}-{}", name, this.currentPoolSize, currentPoolSize);
                this.currentPoolSize = currentPoolSize;
                semaphoreMap.clear();
            }
            if (isolationThresholdPercentage != config.getIsolationThresholdPercentage().get()) {
                double isolationThresholdPercentage = config.getIsolationThresholdPercentage().get();
                logger.info("dynamic-isolation-executor, isolationThresholdPercentage update, name = {}, {}-{}", name, this.isolationThresholdPercentage, isolationThresholdPercentage);
                this.isolationThresholdPercentage = isolationThresholdPercentage;
                semaphoreMap.clear();
            }
            Set<String> set = new HashSet<>(config.getWhiteListIsolationKeys().get());
            if (!set.equals(currentWhiteListIsolationKeys)) {
                logger.info("dynamic-isolation-executor, whiteListIsolationKeys update, name = {}, size {}->{}", name, currentWhiteListIsolationKeys.size(), set.size());
                currentWhiteListIsolationKeys = set;
            }
        } catch (Exception e) {
            logger.error("refresh error, name = {}", name, e);
        }
    }

    private void trySlideStatsWindow() {
        try {
            for (Map.Entry<String, Stats> entry : statsMap.entrySet()) {
                entry.getValue().trySlideToNextBucket();
            }
        } catch (Exception e) {
            logger.error("trySlideStatsWindow error, name = {}", name, e);
        }
    }

    private void afterExecute(Semaphore semaphore, long startMs, String isolationKey) {
        try {
            updateStats(isolationKey, System.currentTimeMillis() - startMs);
            if (semaphore != null) {
                semaphore.release();
            }
        } catch (Exception e) {
            logger.error("afterExecute error, name = {}", name, e);
        }
    }

    /**
     * 获取活跃线程数
     * @return 数量
     */
    public int getActiveCount() {
        int num = 0;
        num += fastExecutor.getActiveCount();
        num += fastBackUpExecutor.getActiveCount();
        num += slowExecutor.getActiveCount();
        num += slowBackUpExecutor.getActiveCount();
        num += isolationExecutor.getActiveCount();
        return num;
    }

    public int getPoolSize() {
        int poolSize = 0;
        poolSize += fastExecutor.getPoolSize();
        poolSize += fastBackUpExecutor.getPoolSize();
        poolSize += slowExecutor.getPoolSize();
        poolSize += slowBackUpExecutor.getPoolSize();
        poolSize += isolationExecutor.getPoolSize();
        return poolSize;
    }

    public int getCompletedTaskCount() {
        int completedTaskCount = 0;
        completedTaskCount += fastExecutor.getCompletedTaskCount();
        completedTaskCount += fastBackUpExecutor.getCompletedTaskCount();
        completedTaskCount += slowExecutor.getCompletedTaskCount();
        completedTaskCount += slowBackUpExecutor.getCompletedTaskCount();
        completedTaskCount += isolationExecutor.getCompletedTaskCount();
        return completedTaskCount;
    }

    /**
     * 获取等待队列的大小
     * @return 大小
     */
    public int getQueueSize() {
        int queueSize = 0;
        queueSize += fastExecutor.getQueue().size();
        queueSize += fastBackUpExecutor.getQueue().size();
        queueSize += slowExecutor.getQueue().size();
        queueSize += slowBackUpExecutor.getQueue().size();
        queueSize += isolationExecutor.getQueue().size();
        return queueSize;
    }

    @Override
    public String getName() {
        return name;
    }

    public CamelliaExecutorStats getStats() {
        CamelliaExecutorStats stats = new CamelliaExecutorStats();
        stats.setActiveThread(getActiveCount());
        stats.setThread(getPoolSize());
        stats.setCompletedTaskCount(getCompletedTaskCount());
        stats.setPendingTask(getQueueSize());
        return stats;
    }

    public CamelliaExecutorStats getExecutorStats(Type type) {
        ThreadPoolExecutor executor;
        if (type == Type.FAST) {
            executor = fastExecutor;
        } else if (type == Type.FAST_BACKUP) {
            executor = fastBackUpExecutor;
        } else if (type == Type.SLOW) {
            executor = slowExecutor;
        } else if (type == Type.SLOW_BACKUP) {
            executor = slowBackUpExecutor;
        } else if (type == Type.WHITE_LIST) {
            executor = whiteListExecutor;
        } else if (type == Type.ISOLATION) {
            executor = isolationExecutor;
        } else {
            return new CamelliaExecutorStats();
        }
        CamelliaExecutorStats stats = new CamelliaExecutorStats();
        stats.setActiveThread(executor.getActiveCount());
        stats.setThread(executor.getPoolSize());
        stats.setCompletedTaskCount(executor.getCompletedTaskCount());
        stats.setPendingTask(executor.getQueue().size());
        return stats;
    }

    public static enum Type {
        FAST,
        FAST_BACKUP,
        SLOW,
        SLOW_BACKUP,
        WHITE_LIST,
        ISOLATION,
        ;
    }

    private static class ExecutorChooseResult {
        Type type;
        ThreadPoolExecutor executor;

        public ExecutorChooseResult(Type type, ThreadPoolExecutor executor) {
            this.type = type;
            this.executor = executor;
        }
    }

    //choose a executor
    private ExecutorChooseResult chooseExecutor(String isolationKey) {
        if (!currentWhiteListIsolationKeys.isEmpty() && currentWhiteListIsolationKeys.contains(isolationKey)) {
            return new ExecutorChooseResult(Type.WHITE_LIST, whiteListExecutor);
        }
        //检查统计信息，如果任务处理的快就用fastExecutor，如果任务处理的慢就用slowExecutor
        Stats stats = getStats(isolationKey);
        if (stats.isFast()) {
            return new ExecutorChooseResult(Type.FAST, fastExecutor);
        } else {
            return new ExecutorChooseResult(Type.SLOW, slowExecutor);
        }
    }

    //更新统计信息
    private void updateStats(String isolationKey, long spendMs) {
        Stats stats = getStats(isolationKey);
        stats.update(spendMs);
    }

    private Semaphore getSemaphore(String isolationKey) {
        Semaphore semaphore = semaphoreMap.get(isolationKey);
        if (semaphore == null) {
            semaphore = semaphoreMap.computeIfAbsent(isolationKey, k -> new Semaphore((int)(currentPoolSize * isolationThresholdPercentage)));
        }
        return semaphore;
    }
    private Stats getStats(String isolationKey) {
        Stats stats = statsMap.get(isolationKey);
        if (stats == null) {
            stats = statsMap.computeIfAbsent(isolationKey, k -> new Stats(config.getStatisticSlidingWindowTime(),
                    config.getStatisticSlidingWindowBucketSize(), config.getTaskSpendThresholdMs()));
        }
        return stats;
    }

    private static class Stats {
        AtomicLong[] spend;
        AtomicLong[] count;

        int index;
        long lastSlideTime;

        boolean fast = true;
        long statisticSlidingWindowTime;
        int statisticSlidingWindowBucketSize;
        DynamicValueGetter<Long> taskSpendThresholdMs;

        public Stats(long statisticSlidingWindowTime, int statisticSlidingWindowBucketSize, DynamicValueGetter<Long> taskSpendThresholdMs) {
            this.statisticSlidingWindowTime = statisticSlidingWindowTime;
            this.statisticSlidingWindowBucketSize = statisticSlidingWindowBucketSize;
            this.taskSpendThresholdMs = taskSpendThresholdMs;
            spend = new AtomicLong[statisticSlidingWindowBucketSize];
            count = new AtomicLong[statisticSlidingWindowBucketSize];
            for (int i = 0; i< statisticSlidingWindowBucketSize; i++) {
                spend[i] = new AtomicLong();
                count[i] = new AtomicLong();
            }
            index = 0;
            lastSlideTime = System.currentTimeMillis();
        }

        synchronized void trySlideToNextBucket() {
            if (System.currentTimeMillis() - lastSlideTime > statisticSlidingWindowTime) {
                if (index == statisticSlidingWindowBucketSize - 1) {
                    index = 0;
                } else {
                    index++;
                }
                spend[index].set(0);
                count[index].set(0);
                lastSlideTime = System.currentTimeMillis();
            }
            checkFast();
        }

        void update(long spendMs) {
            this.count[index].incrementAndGet();
            this.spend[index].addAndGet(spendMs);
        }

        boolean isFast() {
            return fast;
        }

        void checkFast() {
            long totalCount = Arrays.stream(count).mapToLong(AtomicLong::get).sum();
            long totalSpend = Arrays.stream(spend).mapToLong(AtomicLong::get).sum();
            double avgSpend = totalSpend / (totalCount*1.0);
            this.fast = avgSpend < taskSpendThresholdMs.get();
        }
    }
}
