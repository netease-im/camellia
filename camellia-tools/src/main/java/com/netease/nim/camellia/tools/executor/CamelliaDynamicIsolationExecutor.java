package com.netease.nim.camellia.tools.executor;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个可以根据isolationKey自动选择不同线程池的执行器
 *
 * 设计目标：
 * 在一个多租户的系统中，每个租户的表现可能是不一样的，有的租户执行任务快，有的执行任务慢，因此就会导致执行慢的租户影响到执行快的租户
 * 因为系统资源是有限的，因此我们无法通过给每个租户设置一个线程池的方式来做完全的隔离
 * 因此催生了本线程池工具
 * CamelliaDynamicIsolationExecutor的基本原理是通过任务执行的统计数据和线程池的工作状态，动态分配线程资源
 * 目标是执行快的租户不受执行慢的租户的影响，尽可能保证任务执行延迟保持在一个较短的水平
 *
 * 一个典型场景：
 * 每个租户绑定一个http的请求地址，不同租户的http地址响应时间不一样，有的快，有的慢，不同租户的请求量也不一样
 * 我们期望http响应慢的租户不要影响http响应快的租户
 *
 * 内部分为六个线程池：
 * 1）fastExecutor，执行耗时较短的任务
 * 2）fastBackUpExecutor，fastExecutor的backup
 * 3）slowExecutor，执行耗时较长的任务
 * 4）slowBackupExecutor，slowExecutor的backup
 * 5）whiteListExecutor，白名单isolationKey在这里执行，不关心统计数据
 * 6）isolationExecutor，隔离线程池，如果上述五个线程池都执行不了，则最终使用isolationExecutor，如果还是执行不了，则走fallback放弃执行任务
 *
 * 规则：
 * 1）默认走fastExecutor
 * 2）[选择阶段] 如果统计为快（默认阈值1000ms），则使用fastExecutor；如果统计为慢（默认阈值1000ms），则使用slowExecutor
 * 3）[选择阶段] 如果fastExecutor/slowExecutor任务执行延迟超过阈值（默认300ms），且fastBackUpExecutor/slowBackupExecutor的延迟小于fastExecutor/slowExecutor，则使用fastBackUpExecutor/slowBackupExecutor
 * 4）[提交阶段] 如果因为fastExecutor/slowExecutor繁忙而提交失败，则进入fastBackUpExecutor/slowBackupExecutor，如果仍然繁忙，则转交给isolationExecutor
 * 5）[执行阶段] 如果某个isolationKey的最新统计数据和当前线程池不匹配，则转交给匹配的线程池
 * 6）[执行阶段] 如果某个线程池执行任务延迟超过阈值（默认300ms），且其他线程池有空闲的（有空闲的线程），则转交给其他线程池（fast会找fastBackup+isolation，fastBackup会找fast+isolation，slow会找slowBackup+isolation，slowBackup会找slow+isolation）
 * 7）[执行阶段] 如果某个isolationKey在fastExecutor/slowExecutor中占有线程数比例超过阈值（默认0.3），则转交给fastBackUpExecutor/slowBackupExecutor执行
 * 8）[执行阶段] 如果某个isolationKey在fastBackUpExecutor/slowBackupExecutor占有线程数比例也超过阈值（默认0.3），则转交给isolationExecutor执行
 * 9）[选择阶段] 在白名单列表里的isolationKey，直接在whiteListExecutor中执行；[提交阶段] 如果whiteListExecutor繁忙，则转交给isolationExecutor
 * 10）最终所有任务都会把isolationExecutor作为兜底，如果isolationExecutor因为繁忙处理不了任务，则走fallback回调告诉任务提交者任务被放弃执行了
 * 11）可以设置任务过期时间（默认不过期），任务如果过期而被放弃也会走fallback
 * 12）fallback方法务必不要有阻塞，fallback方法会告知任务不执行的原因（当前定义了2个原因：任务已过期、任务被拒绝）
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

    private static final String LATENCY_TAG = "latency";
    private final CamelliaLocalCache latencyCache = new CamelliaLocalCache(Type.values().length * 2);

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

    public static final Fallback dummyFallback = (isolationKey, reason) -> {};
    public static final Fallback loggingFallback = (isolationKey, reason) -> logger.warn("task of isolationKey = {} discard, reason = {}", isolationKey, reason);

    public static enum FallbackReason {
        TASK_EXPIRE,
        TASK_REJECT,
        ;
    }

    public static interface Fallback {
        void fallback(String isolationKey, FallbackReason reason);
    }

    /**
     * 提交一个任务
     * @param isolationKey 隔离key
     * @param callable 任务
     */
    public <T> Future<T> submit(String isolationKey, Callable<T> callable) {
        return submit(isolationKey, callable, loggingFallback);
    }

    /**
     * 提交一个任务
     * @param isolationKey 隔离key
     * @param callable 任务
     * @param fallback 任务未执行时的fallback回调
     */
    public <T> Future<T> submit(String isolationKey, Callable<T> callable, Fallback fallback) {
        FutureTask<T> task = new FutureTask<>(callable);
        TaskWrapper<T> taskWrapper = new TaskWrapper<>(task, config.getTaskExpireTimeMs().get());
        submit0(isolationKey, taskWrapper, fallback);
        return task;
    }

    /**
     * 提交一个任务
     * @param isolationKey 隔离key
     * @param runnable 任务
     */
    public void submit(String isolationKey, Runnable runnable) {
        submit(isolationKey, runnable, loggingFallback);
    }

    /**
     * 提交一个任务
     * @param isolationKey 隔离key
     * @param runnable 任务
     * @param fallback 任务未执行时的fallback回调
     */
    public Future<Void> submit(String isolationKey, Runnable runnable, Fallback fallback) {
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        TaskWrapper<Void> taskWrapper = new TaskWrapper<>(task, config.getTaskExpireTimeMs().get());
        submit0(isolationKey, taskWrapper, fallback);
        return task;
    }

    //submit FutureTask
    private void submit0(String isolationKey, TaskWrapper<?> task, Fallback fallback) {
        ExecutorChooseResult result = chooseExecutor(isolationKey);
        doTask(isolationKey, result.executor, result.type, task, fallback, 0);
    }

    private void doTask(String isolationKey, ThreadPoolExecutor executor, Type type, TaskWrapper<?> task, Fallback fallback, int depth) {
        if (task.isExpire()) {
            fallback.fallback(isolationKey, FallbackReason.TASK_EXPIRE);
            return;
        }
        try {
            doTask0(isolationKey, executor, task, type, fallback, depth);
        } catch (RejectedExecutionException e) {
            if (type == Type.FAST) {
                try {
                    doTask0(isolationKey, fastBackUpExecutor, task, Type.FAST_BACKUP, fallback, depth);
                } catch (RejectedExecutionException ex) {
                    try {
                        doTask0(isolationKey, isolationExecutor, task, Type.ISOLATION, fallback, depth);
                    } catch (RejectedExecutionException exc) {
                        fallback.fallback(isolationKey, FallbackReason.TASK_REJECT);
                    }
                }
                return;
            }
            if (type == Type.SLOW) {
                try {
                    doTask0(isolationKey, slowBackUpExecutor, task, Type.SLOW_BACKUP, fallback, depth);
                } catch (RejectedExecutionException ex) {
                    try {
                        doTask0(isolationKey, isolationExecutor, task, Type.ISOLATION, fallback, depth);
                    } catch (RejectedExecutionException exc) {
                        fallback.fallback(isolationKey, FallbackReason.TASK_REJECT);
                    }
                }
                return;
            }
            if (type != Type.ISOLATION) {
                try {
                    doTask0(isolationKey, isolationExecutor, task, Type.ISOLATION, fallback, depth);
                } catch (RejectedExecutionException exc) {
                    fallback.fallback(isolationKey, FallbackReason.TASK_REJECT);
                }
            } else {
                fallback.fallback(isolationKey, FallbackReason.TASK_REJECT);
            }
        }
    }

    private void doTask0(String isolationKey, ThreadPoolExecutor executor, TaskWrapper<?> task, Type type, Fallback fallback, int depth) {
        executor.submit(() -> {
            Semaphore semaphore = null;
            if (type != Type.ISOLATION && depth < config.getMaxDepth().get()) {
                //check stats
                Type nextType = null;
                ThreadPoolExecutor nextExecutor = null;
                Stats stats = getStats(isolationKey);
                if (type == Type.FAST || type == Type.FAST_BACKUP) {
                    if (!stats.isFast()) {
                        nextType = Type.SLOW;
                        nextExecutor = slowExecutor;
                    }
                } else if (type == Type.SLOW || type == Type.SLOW_BACKUP) {
                    if (stats.isFast()) {
                        nextType = Type.FAST;
                        nextExecutor = fastExecutor;
                    }
                }
                if (nextType != null && nextExecutor != null) {
                    doTask(isolationKey, nextExecutor, nextType, task, fallback, depth + 1);
                    return;
                }
                //check latency
                if (task.getLatency() > config.getTargetLatencyMs().get()) {
                    List<ExecutorChooseResult> list = checkTargetIdleExecutors(type);
                    if (list != null) {
                        for (ExecutorChooseResult result : list) {
                            if (result.executor.getActiveCount() < result.executor.getCorePoolSize() && result.executor.getQueue().isEmpty()) {
                                doTask(isolationKey, result.executor, result.type, task, fallback, depth + 1);
                                return;
                            }
                        }
                    }
                }
                //check semaphore
                semaphore = getSemaphore(isolationKey, type);
                if (!semaphore.tryAcquire()) {
                    if (type == Type.FAST) {
                        nextExecutor = fastBackUpExecutor;
                        nextType = Type.FAST_BACKUP;
                    } else if (type == Type.SLOW) {
                        nextExecutor = slowBackUpExecutor;
                        nextType = Type.SLOW_BACKUP;
                    } else {
                        nextExecutor = isolationExecutor;
                        nextType = Type.ISOLATION;
                    }
                    doTask(isolationKey, nextExecutor, nextType, task, fallback, depth + 1);
                    return;
                }
            }
            doTask0(isolationKey, semaphore, task, type, fallback);
        });
    }

    private void doTask0(String isolationKey, Semaphore semaphore, TaskWrapper<?> task, Type type, Fallback fallback) {
        latencyCache.put(LATENCY_TAG, type.name(), task.getLatency(), config.getTaskSpendThresholdMs().get() * 32);
        currentType.set(type);
        if (task.isExpire()) {
            fallback.fallback(isolationKey, FallbackReason.TASK_EXPIRE);
            return;
        }
        long startMs = System.currentTimeMillis();
        try {
            task.run();
        } catch (Exception e) {
            logger.error("error", e);
        } finally {
            afterExecute(semaphore, startMs, isolationKey);
        }
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

    private List<ExecutorChooseResult> checkTargetIdleExecutors(Type type) {
        ExecutorChooseResult fast = new ExecutorChooseResult(Type.FAST, fastBackUpExecutor);
        ExecutorChooseResult fastBackup = new ExecutorChooseResult(Type.FAST_BACKUP, fastBackUpExecutor);
        ExecutorChooseResult slow = new ExecutorChooseResult(Type.SLOW, slowExecutor);
        ExecutorChooseResult slowBackup = new ExecutorChooseResult(Type.SLOW_BACKUP, slowBackUpExecutor);
        ExecutorChooseResult isolation = new ExecutorChooseResult(Type.ISOLATION, isolationExecutor);
        if (type == Type.FAST) {
            return Arrays.asList(fastBackup, isolation);
        } else if (type == Type.FAST_BACKUP) {
            return Arrays.asList(fast, isolation);
        } else if (type == Type.SLOW) {
            return Arrays.asList(slowBackup, isolation);
        } else if (type == Type.SLOW_BACKUP) {
            return Arrays.asList(slow, isolation);
        }
        return null;
    }

    private static class TaskWrapper<T> {
        FutureTask<T> futureTask;
        final long createTime;
        final long taskExpireTimeMs;
        public TaskWrapper(FutureTask<T> futureTask, int taskExpireTimeMs) {
            this.futureTask = futureTask;
            this.createTime = System.currentTimeMillis();
            if (taskExpireTimeMs < 0) {
                this.taskExpireTimeMs = Long.MAX_VALUE;
            } else {
                this.taskExpireTimeMs = createTime + taskExpireTimeMs;
            }
        }

        public void run() {
            futureTask.run();
        }

        public long getLatency() {
            return System.currentTimeMillis() - createTime;
        }

        public boolean isExpire() {
            return System.currentTimeMillis() > taskExpireTimeMs;
        }

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
            Long latency1 = latencyCache.get(LATENCY_TAG, Type.FAST.name(), Long.class);
            if (latency1 == null || latency1 < config.getTargetLatencyMs().get()
                    || fastExecutor.getActiveCount() < fastExecutor.getCorePoolSize() || fastExecutor.getQueue().isEmpty()) {
                return new ExecutorChooseResult(Type.FAST, fastExecutor);
            }
            Long latency2 = latencyCache.get(LATENCY_TAG, Type.FAST_BACKUP.name(), Long.class);
            if (latency2 == null || latency1 < latency2
                    || fastBackUpExecutor.getActiveCount() < fastBackUpExecutor.getCorePoolSize() || fastBackUpExecutor.getQueue().isEmpty()) {
                return new ExecutorChooseResult(Type.FAST_BACKUP, fastBackUpExecutor);
            }
            return new ExecutorChooseResult(Type.FAST, fastExecutor);
        } else {
            Long latency1 = latencyCache.get(LATENCY_TAG, Type.SLOW.name(), Long.class);
            if (latency1 == null || latency1 < config.getTargetLatencyMs().get()
                    || slowExecutor.getActiveCount() < slowExecutor.getCorePoolSize() || slowExecutor.getQueue().isEmpty()) {
                return new ExecutorChooseResult(Type.SLOW, slowExecutor);
            }
            Long latency2 = latencyCache.get(LATENCY_TAG, Type.SLOW_BACKUP.name(), Long.class);
            if (latency2 == null || latency1 < latency2
                    || slowBackUpExecutor.getActiveCount() < slowBackUpExecutor.getCorePoolSize() || slowBackUpExecutor.getQueue().isEmpty()) {
                return new ExecutorChooseResult(Type.SLOW_BACKUP, slowBackUpExecutor);
            }
            return new ExecutorChooseResult(Type.SLOW, slowExecutor);
        }
    }

    //更新统计信息
    private void updateStats(String isolationKey, long spendMs) {
        Stats stats = getStats(isolationKey);
        stats.update(spendMs);
    }

    private Semaphore getSemaphore(String isolationKey, Type type) {
        String key = type + "|" + isolationKey;
        Semaphore semaphore = semaphoreMap.get(key);
        if (semaphore == null) {
            semaphore = semaphoreMap.computeIfAbsent(key, k -> new Semaphore((int)(currentPoolSize * isolationThresholdPercentage)));
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
