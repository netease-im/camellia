package com.netease.nim.camellia.tools.executor;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 不同于ScheduledExecutorService
 * 1) 如果设置1s执行一次，但是本身执行了2s，那么实际任务执行间隔是3s，而不是2s
 * 2) 任务如果报错，会继续执行，而不是中断
 * Created by caojiajun on 2023/4/23
 */
public class CamelliaScheduleExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaScheduleExecutor.class);

    private final String name;
    private final HashedWheelTimer timer;
    private final ThreadPoolExecutor executor;

    public CamelliaScheduleExecutor(String name, int poolSize) {
        this.name = name;
        this.timer = new HashedWheelTimer();
        this.executor = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024*128), new CamelliaThreadFactory(name), new ThreadPoolExecutor.AbortPolicy());
    }

    public Task scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Task task = new Task(name, command, timer, executor);
        task.timeout = timer.newTimeout(timeout -> task.tryInvoke(period, unit), initialDelay, unit);
        return task;
    }

    public static class Task {
        private final AtomicBoolean cancel = new AtomicBoolean(false);

        private final String name;
        private final Runnable command;
        private final HashedWheelTimer timer;
        private final ThreadPoolExecutor executor;
        private Timeout timeout;

        public Task(String name, Runnable command, HashedWheelTimer timer, ThreadPoolExecutor executor) {
            this.name = name;
            this.command = command;
            this.timer = timer;
            this.executor = executor;
        }

        /**
         * 取消任务
         */
        public void cancel() {
            cancel.set(true);
            if (timeout != null) {
                timeout.cancel();
            }
        }

        private void tryInvoke(long delay, TimeUnit unit) {
            if (cancel.get()) return;
            try {
                executor.submit(() -> {
                    try {
                        command.run();
                    } catch (Throwable e) {
                        logger.error("{} invoke error", Task.this.name, e);
                    } finally {
                        if (!cancel.get()) {
                            Task.this.timeout = Task.this.addDelayQueue(delay, unit);
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("{} submit error", name, e);
                if (!cancel.get()) {
                    Task.this.timeout = Task.this.addDelayQueue(delay, unit);
                }
            }
        }

        private Timeout addDelayQueue(long delay, TimeUnit unit) {
            return timer.newTimeout(timeout -> Task.this.tryInvoke(delay, unit), delay, unit);
        }
    }
}
