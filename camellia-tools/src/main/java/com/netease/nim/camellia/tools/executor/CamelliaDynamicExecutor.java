package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2023/1/3
 */
public class CamelliaDynamicExecutor implements ExecutorService, CamelliaExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDynamicExecutor.class);

    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
            new CamelliaThreadFactory("camellia-dynamic-executor-schedule"));

    private final String name;
    private final ThreadPoolExecutor executor;

    private final CamelliaDynamicExecutorConfig config;

    private final DynamicValueGetter<Integer> corePoolSize;
    private final DynamicValueGetter<Integer> maxPoolSize;
    private final DynamicValueGetter<Long> keepAliveTime;
    private final DynamicValueGetter<TimeUnit> unit;
    private final DynamicValueGetter<RejectedExecutionHandler> rejectedExecutionHandler;

    public CamelliaDynamicExecutor(String name, int poolSize) {
        this(new CamelliaDynamicExecutorConfig(name, () -> poolSize, () -> poolSize));
    }

    public CamelliaDynamicExecutor(String name, int poolSize, int queueSize) {
        this(new CamelliaDynamicExecutorConfig(name, () -> poolSize, () -> poolSize, () -> queueSize));
    }

    public CamelliaDynamicExecutor(CamelliaDynamicExecutorConfig config) {
        this.name = CamelliaExecutorMonitor.genExecutorName(config.getName());
        this.config = config;
        this.corePoolSize = config.getCorePoolSize();
        this.maxPoolSize = config.getMaxPoolSize();
        this.keepAliveTime = config.getKeepAliveTime();
        this.unit = config.getUnit();
        this.rejectedExecutionHandler = config.getRejectedExecutionHandler();

        this.executor = new ThreadPoolExecutor(corePoolSize.get(), maxPoolSize.get(), keepAliveTime.get(),
                unit.get(), new DynamicCapacityLinkedBlockingQueue<>(config.getQueueSize()),
                new CamelliaThreadFactory("[camellia-dynamic-executor][" + name + "]"), config.getRejectedExecutionHandler().get());

        scheduledExecutor.scheduleAtFixedRate(this::refresh, 10, 10, TimeUnit.SECONDS);

        CamelliaExecutorMonitor.register(this);
    }

    private void refresh() {
        try {
            if (executor.getCorePoolSize() != corePoolSize.get()) {
                Integer size = corePoolSize.get();
                logger.info("dynamic-executor, corePoolSize update, name = {}, {}->{}", name, executor.getCorePoolSize(), size);
                executor.setCorePoolSize(size);
            }
            if (executor.getMaximumPoolSize() != maxPoolSize.get()) {
                Integer size = maxPoolSize.get();
                logger.info("dynamic-executor, maxPoolSize update, name = {}, {}->{}", name, executor.getCorePoolSize(), size);
                executor.setMaximumPoolSize(size);
            }
            if (executor.getKeepAliveTime(TimeUnit.NANOSECONDS) != unit.get().toNanos(keepAliveTime.get())) {
                Long time = keepAliveTime.get();
                TimeUnit timeUnit = unit.get();
                logger.info("dynamic-executor, keepAliveTime update, name = {}, {}->{} in nano-seconds", name, executor.getKeepAliveTime(TimeUnit.NANOSECONDS), timeUnit.toNanos(time));
                executor.setKeepAliveTime(time, timeUnit);
            }
            if (!rejectedExecutionHandler.get().getClass().equals(executor.getRejectedExecutionHandler().getClass())) {
                RejectedExecutionHandler handler = this.rejectedExecutionHandler.get();
                logger.info("dynamic-executor, rejectedExecutionHandler update, name = {}, {}->{}", name, executor.getRejectedExecutionHandler().getClass().getName(), handler.getClass().getName());
                executor.setRejectedExecutionHandler(handler);
            }
        } catch (Exception e) {
            logger.error("refresh error, name = {}", name, e);
        }
    }

    public CamelliaDynamicExecutorConfig getConfig() {
        return config;
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public CamelliaExecutorStats getStats() {
        CamelliaExecutorStats stats = new CamelliaExecutorStats();
        stats.setActiveThread(executor.getActiveCount());
        stats.setThread(executor.getPoolSize());
        stats.setCompletedTaskCount(executor.getCompletedTaskCount());
        stats.setPendingTask(executor.getQueue().size());
        return stats;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return executor.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public String getName() {
        return name;
    }
}
