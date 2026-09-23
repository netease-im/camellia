package com.netease.nim.camellia.mq.isolation.core.executor;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Creates one executor according to startup configuration; its mode is immutable afterwards. */
final class MsgTaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MsgTaskExecutor.class);

    enum Mode {
        PLATFORM("platform"),
        VIRTUAL("virtual");

        private final String value;

        Mode(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    interface ExecutorFactory {
        ExecutorService createPlatform(String name, int threads);

        ExecutorService createVirtual();
    }

    private static final class DefaultExecutorFactory implements ExecutorFactory {
        @Override
        public ExecutorService createPlatform(String name, int threads) {
            return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new CamelliaThreadFactory("camellia-mq-isolation[" + name + "]"),
                    new ThreadPoolExecutor.AbortPolicy());
        }

        @Override
        public ExecutorService createVirtual() {
            return VirtualThreadExecutorFactory.create();
        }
    }

    private static final class Holder {
        private final Mode mode;
        private final ExecutorService executor;
        private final Semaphore virtualPermits;
        private final AtomicInteger activeTasks = new AtomicInteger();

        private Holder(Mode mode, ExecutorService executor, int threads) {
            this.mode = mode;
            this.executor = executor;
            this.virtualPermits = mode == Mode.VIRTUAL ? new Semaphore(threads) : null;
        }
    }

    private final String name;
    private final Holder holder;
    private final AtomicBoolean shutdownRejectLogged = new AtomicBoolean();
    private volatile boolean shutdown;

    MsgTaskExecutor(String name, int threads, boolean virtualThreadEnable) {
        this(name, threads, virtualThreadEnable, new DefaultExecutorFactory());
    }

    MsgTaskExecutor(String name, int threads, boolean virtualThreadEnable,
                    ExecutorFactory executorFactory) {
        if (threads <= 0) {
            throw new IllegalArgumentException("threads must be greater than 0, name=" + name + ", threads=" + threads);
        }
        this.name = name;
        Mode mode = virtualThreadEnable ? Mode.VIRTUAL : Mode.PLATFORM;
        this.holder = new Holder(mode, createExecutor(mode, name, threads, executorFactory), threads);
    }

    /**
     * Submits the task, falling back to inline execution to preserve the fixed-pool backpressure semantics.
     *
     * @return true when the task was handed to the executor or executed inline,
     *         false when the executor is shutting down or rejected the task without executing it.
     */
    boolean execute(Runnable task) {
        if (shutdown) {
            logShutdownReject();
            return false;
        }
        if (holder.virtualPermits != null && !acquireVirtualPermit()) {
            return false;
        }
        Runnable wrapped = wrap(task);
        try {
            holder.executor.execute(wrapped);
            return true;
        } catch (RejectedExecutionException e) {
            if (holder.executor.isShutdown()) {
                releaseVirtualPermit();
                logShutdownReject();
                return false;
            }
        } catch (Throwable e) {
            // The task was not handed over, so the permit acquired above must be returned.
            releaseVirtualPermit();
            throw e;
        }
        // Preserve fixed-pool CallerRuns backpressure without interrupting the task.
        wrapped.run();
        return true;
    }

    private boolean acquireVirtualPermit() {
        try {
            holder.virtualPermits.acquire();
            return true;
        } catch (InterruptedException e) {
            // Let the caller route the message to the isolation mq instead of blocking shutdown forever.
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void logShutdownReject() {
        if (shutdownRejectLogged.compareAndSet(false, true)) {
            logger.warn("executor is shutdown, task will be routed to isolation mq, name={}", name);
        }
    }

    private Runnable wrap(Runnable task) {
        return () -> {
            holder.activeTasks.incrementAndGet();
            try {
                task.run();
            } finally {
                holder.activeTasks.decrementAndGet();
                releaseVirtualPermit();
            }
        };
    }

    private void releaseVirtualPermit() {
        if (holder.virtualPermits != null) {
            holder.virtualPermits.release();
        }
    }

    private static ExecutorService createExecutor(Mode mode, String name, int threads,
                                                   ExecutorFactory executorFactory) {
        return mode == Mode.VIRTUAL
                ? executorFactory.createVirtual()
                : executorFactory.createPlatform(name, threads);
    }

    String getMode() {
        return holder.mode.value();
    }

    int getCurrentThreads() {
        if (holder.executor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) holder.executor).getPoolSize();
        }
        return 0;
    }

    int getActiveTasks() {
        if (holder.executor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) holder.executor).getActiveCount();
        }
        return holder.activeTasks.get();
    }

    void shutdown() {
        shutdown(30, TimeUnit.SECONDS);
    }

    void shutdown(long timeout, TimeUnit unit) {
        if (shutdown) {
            return;
        }
        shutdown = true;
        holder.executor.shutdown();
        try {
            long remaining = unit.toNanos(timeout);
            if (remaining > 0 && !holder.executor.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
                logger.warn("executor did not terminate before timeout, name={}", name);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("interrupted while waiting for executor termination, name={}", name);
        }
    }
}
