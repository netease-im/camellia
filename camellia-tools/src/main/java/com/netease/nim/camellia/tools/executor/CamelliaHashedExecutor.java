package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2021/8/9
 */
public class CamelliaHashedExecutor implements CamelliaExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHashedExecutor.class);

    public static final int defaultPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    public static final int defaultQueueSize = 100000;
    public static final RejectedExecutionHandler defaultRejectedPolicy = new AbortPolicy();

    private final AtomicLong workerIdGen = new AtomicLong(1);

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean initOk = new AtomicBoolean(false);

    private final String name;
    private final int poolSize;
    private int queueSize;
    private DynamicValueGetter<Integer> dynamicQueueSize;
    private final List<WorkThread> workThreads;
    private final DynamicValueGetter<RejectedExecutionHandler> rejectedExecutionHandler;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public CamelliaHashedExecutor(String name) {
        this(name, defaultPoolSize, defaultQueueSize, defaultRejectedPolicy);
    }

    public CamelliaHashedExecutor(String name, int poolSize) {
        this(name, poolSize, defaultQueueSize, defaultRejectedPolicy);
    }

    public CamelliaHashedExecutor(String name, int poolSize, DynamicValueGetter<Integer> dynamicQueueSize) {
        this(name, poolSize, dynamicQueueSize, defaultRejectedPolicy);
    }

    public CamelliaHashedExecutor(String name, int poolSize, int queueSize) {
        this(name, poolSize, queueSize, defaultRejectedPolicy);
    }

    public CamelliaHashedExecutor(String name, RejectedExecutionHandler rejectedExecutionHandler) {
        this(name, defaultPoolSize, defaultQueueSize, rejectedExecutionHandler);
    }

    public CamelliaHashedExecutor(String name, int poolSize, DynamicValueGetter<Integer> dynamicQueueSize, RejectedExecutionHandler rejectedExecutionHandler) {
        this(new CamelliaHashedExecutorConfig(name, poolSize, dynamicQueueSize, () -> rejectedExecutionHandler));
    }

    public CamelliaHashedExecutor(CamelliaHashedExecutorConfig config) {
        this.name = CamelliaExecutorMonitor.genExecutorName(config.getName());
        this.poolSize = config.getPoolSize();
        this.dynamicQueueSize = config.getDynamicQueueSize();
        this.workThreads = new ArrayList<>(poolSize);
        this.rejectedExecutionHandler = config.getRejectedExecutionHandler();

        CamelliaExecutorMonitor.register(this);
    }

    public CamelliaHashedExecutor(String name, int poolSize, int queueSize, RejectedExecutionHandler rejectedExecutionHandler) {
        this.name = CamelliaExecutorMonitor.genExecutorName(name);
        this.poolSize = poolSize;
        this.queueSize = queueSize;
        this.workThreads = new ArrayList<>(poolSize);
        this.rejectedExecutionHandler = () -> rejectedExecutionHandler;

        CamelliaExecutorMonitor.register(this);
    }

    private void initWorkThreads() {
        if (initOk.get()) return;
        lock.lock();
        try {
            if (!initOk.get()) {
                for (int i = 0; i < poolSize; i++) {
                    WorkThread workThread;
                    if (dynamicQueueSize != null) {
                        workThread = new WorkThread(this, dynamicQueueSize);
                    } else {
                        workThread = new WorkThread(this, queueSize);
                    }
                    workThread.start();
                    workThreads.add(workThread);
                }
                logger.info("CamelliaHashedExecutor thread init success, name = {}, poolSize = {}, queueSize = {}, rejectedExecutionHandler = {}",
                        name, poolSize, queueSize, rejectedExecutionHandler.getClass().getSimpleName());
                initOk.set(true);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据hashKey选取一个固定的工作线程执行一个任务
     * @param hashKey hashKey
     * @param runnable 无返回结果的任务
     * @return 任务结果
     */
    public Future<Void> submit(byte[] hashKey, Runnable runnable) {
        if (shutdown.get()) {
            throw new IllegalStateException("executor has shutdown");
        }
        initWorkThreads();
        int index = Math.abs(Arrays.hashCode(hashKey)) % workThreads.size();
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        boolean success = workThreads.get(index).submit(task);
        if (!success) {
            rejectedExecutionHandler.get().rejectedExecution(task, this);
        }
        return task;
    }

    public Future<Void> submit(String hashKey, Runnable runnable) {
        return submit(hashKey.getBytes(StandardCharsets.UTF_8), runnable);
    }

    /**
     * 根据hashKey选取一个固定的工作线程执行一个任务
     * @param hashKey hashKey
     * @param callable 有返回结果的任务
     * @return 任务结果
     */
    public <T> Future<T> submit(byte[] hashKey, Callable<T> callable) {
        if (shutdown.get()) {
            throw new IllegalStateException("executor has shutdown");
        }
        initWorkThreads();
        int index = Math.abs(Arrays.hashCode(hashKey)) % workThreads.size();
        FutureTask<T> task = new FutureTask<>(callable);
        boolean success = workThreads.get(index).submit(task);
        if (!success) {
            rejectedExecutionHandler.get().rejectedExecution(task, this);
        }
        return task;
    }

    public <T> Future<T> submit(String hashKey, Callable<T> callable) {
        return submit(hashKey.getBytes(StandardCharsets.UTF_8), callable);
    }

    @Override
    public String getName() {
        return name;
    }

    public int getPoolSize() {
        return poolSize;
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        this.shutdown.set(true);
    }

    /**
     * 线程池是否已关闭
     * @return 是否已关闭
     */
    public boolean isShutdown() {
        return this.shutdown.get();
    }

    /**
     * 获取活跃线程数
     * @return 数量
     */
    public int getActiveCount() {
        int num = 0;
        for (WorkThread workThread : workThreads) {
            if (workThread.isActive()) {
                num ++;
            }
        }
        return num;
    }

    /**
     * 获取某个hashKey下的等待队列大小
     * @param hashKey hashKey
     * @return 大小
     */
    public int getQueueSize(String hashKey) {
        return getQueueSize(hashKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获取某个hashKey下的等待队列大小
     * @param hashKey hashKey
     * @return 大小
     */
    public int getQueueSize(byte[] hashKey) {
        if (!initOk.get()) return 0;
        int index = Math.abs(Arrays.hashCode(hashKey)) % workThreads.size();
        WorkThread workThread = workThreads.get(index);
        return workThread.queueSize();
    }

    /**
     * 获取等待队列的大小
     * @return 大小
     */
    public int getQueueSize() {
        int queueSize = 0;
        for (WorkThread workThread : workThreads) {
            queueSize += workThread.queueSize();
        }
        return queueSize;
    }

    /**
     * 获取完成的任务数量
     * @return 数量
     */
    public long getCompletedTaskCount() {
        long completedTaskCount = 0;
        for (WorkThread workThread : workThreads) {
            completedTaskCount += workThread.getCompletedTaskCount();
        }
        return completedTaskCount;
    }

    public CamelliaExecutorStats getStats() {
        CamelliaExecutorStats stats = new CamelliaExecutorStats();
        if (initOk.get()) {
            stats.setActiveThread(getActiveCount());
            stats.setThread(workThreads.size());
            stats.setCompletedTaskCount(getCompletedTaskCount());
            stats.setPendingTask(getQueueSize());
        } else {
            stats.setActiveThread(0);
            stats.setThread(0);
            stats.setPendingTask(0);
            stats.setCompletedTaskCount(0);
        }
        return stats;
    }

    public static interface RejectedExecutionHandler {
        void rejectedExecution(Runnable runnable, CamelliaHashedExecutor executor);
    }

    public static class AbortPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, CamelliaHashedExecutor executor) {
            throw new RejectedExecutionException("Task " + runnable.toString() + " rejected from " + executor.name);
        }
    }

    public static class DiscardPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, CamelliaHashedExecutor executor) {
            logger.warn("Task " + runnable.toString() + " is discard from " + executor.name);
        }
    }

    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, CamelliaHashedExecutor executor) {
            runnable.run();
        }
    }

    private static class WorkThread extends Thread {

        private final BlockingQueue<FutureTask<?>> queue;
        private final CamelliaHashedExecutor executor;
        private final AtomicBoolean active = new AtomicBoolean(false);

        private final AtomicLong completedTaskCount = new AtomicLong(0);

        public WorkThread(CamelliaHashedExecutor executor, int queueSize) {
            this.queue = new LinkedBlockingQueue<>(queueSize);
            this.executor = executor;
            setName("camellia-hashed-executor-" + executor.getName() + "-" + executor.workerIdGen.getAndIncrement());
        }

        public WorkThread(CamelliaHashedExecutor executor, DynamicValueGetter<Integer> dynamicQueueSize) {
            this.queue = new DynamicCapacityLinkedBlockingQueue<>(dynamicQueueSize);
            this.executor = executor;
            setName("camellia-hashed-executor-" + executor.getName() + "-" + executor.workerIdGen.getAndIncrement());
        }

        public boolean submit(FutureTask<?> task) {
            return queue.offer(task);
        }

        public boolean isActive() {
            return active.get() || !queue.isEmpty();
        }

        public int queueSize() {
            return queue.size();
        }

        public long getCompletedTaskCount() {
            return completedTaskCount.get();
        }

        @Override
        public void run() {
            while (!executor.shutdown.get()) {
                try {
                    FutureTask<?> task = queue.poll(1, TimeUnit.SECONDS);
                    if (task != null) {
                        active.set(true);
                        try {
                            task.run();
                        } finally {
                            active.set(false);
                            completedTaskCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    logger.error("error", e);
                }
            }
            int size = queue.size();
            logger.warn("camellia hashed executor work thread shutdown, thread.name = {}, skip.task.size = {}", getName(), size);
        }
    }
}
