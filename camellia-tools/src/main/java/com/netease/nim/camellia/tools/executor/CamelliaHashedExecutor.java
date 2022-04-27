package com.netease.nim.camellia.tools.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2021/8/9
 */
public class CamelliaHashedExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHashedExecutor.class);

    private static final int defaultPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private static final int defaultQueueSize = 100000;
    private static final RejectedExecutionHandler defaultRejectedPolicy = new AbortPolicy();

    private static final AtomicLong executorIdGen = new AtomicLong(1);
    private final AtomicLong workerIdGen = new AtomicLong(1);

    private final String name;
    private final int poolSize;
    private final List<WorkThread> workThreads;
    private final RejectedExecutionHandler rejectedExecutionHandler;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public CamelliaHashedExecutor(String name) {
        this(name, defaultPoolSize, defaultQueueSize, defaultRejectedPolicy);
    }

    public CamelliaHashedExecutor(String name, int poolSize) {
        this(name, poolSize, defaultQueueSize, defaultRejectedPolicy);
    }

    public CamelliaHashedExecutor(String name, int poolSize, int queueSize) {
        this(name, poolSize, queueSize, defaultRejectedPolicy);
    }

    public CamelliaHashedExecutor(String name, RejectedExecutionHandler rejectedExecutionHandler) {
        this(name, defaultPoolSize, defaultQueueSize, rejectedExecutionHandler);
    }

    public CamelliaHashedExecutor(String name, int poolSize, int queueSize, RejectedExecutionHandler rejectedExecutionHandler) {
        this.name = name + "-" + executorIdGen.getAndIncrement();
        this.poolSize = poolSize;
        this.workThreads = new ArrayList<>(poolSize);
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        for (int i=0; i<poolSize; i++) {
            WorkThread workThread = new WorkThread(this, queueSize);
            workThread.start();
            workThreads.add(workThread);
        }
        logger.info("CamelliaHashedExecutor init success, name = {}, poolSize = {}, queueSize = {}, rejectedExecutionHandler = {}",
                name, poolSize, queueSize, rejectedExecutionHandler.getClass().getSimpleName());
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
        int index = Math.abs(Arrays.hashCode(hashKey)) % workThreads.size();
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        boolean success = workThreads.get(index).submit(task);
        if (!success) {
            rejectedExecutionHandler.rejectedExecution(task, this);
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
        int index = Math.abs(Arrays.hashCode(hashKey)) % workThreads.size();
        FutureTask<T> task = new FutureTask<>(callable);
        boolean success = workThreads.get(index).submit(task);
        if (!success) {
            rejectedExecutionHandler.rejectedExecution(task, this);
        }
        return task;
    }

    public <T> Future<T> submit(String hashKey, Callable<T> callable) {
        return submit(hashKey.getBytes(StandardCharsets.UTF_8), callable);
    }

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

        private final LinkedBlockingQueue<FutureTask<?>> queue;
        private final CamelliaHashedExecutor executor;
        private final AtomicBoolean active = new AtomicBoolean(false);

        public WorkThread(CamelliaHashedExecutor executor, int queueSize) {
            this.queue = new LinkedBlockingQueue<>(queueSize);
            this.executor = executor;
            setName("camellia-hashed-executor-" + executor.getName() + "-" + executor.workerIdGen.getAndIncrement());
        }

        public boolean submit(FutureTask<?> task) {
            return queue.offer(task);
        }

        public boolean isActive() {
            return active.get() || !queue.isEmpty();
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
