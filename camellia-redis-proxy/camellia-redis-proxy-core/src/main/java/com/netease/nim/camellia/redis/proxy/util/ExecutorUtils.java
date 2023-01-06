package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2020/11/11
 */
public class ExecutorUtils {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorUtils.class);

    private static final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(SysUtils.getCpuNum(), new DefaultThreadFactory("camellia-schedule"));
    private static final HashedWheelTimer delayTimer = new HashedWheelTimer(new DefaultThreadFactory("camellia-delay-timer"));
    private static final ExecutorService singleThreadExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10240));
    private static final ThreadPoolExecutor callbackExecutor = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(),
            0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000), new DefaultThreadFactory("camellia-callback"), new ThreadPoolExecutor.AbortPolicy());

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable task,
                                                         long initialDelay,
                                                         long period,
                                                         TimeUnit unit) {
        return scheduleService.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public static void submitDelayTask(Runnable runnable, long delay, TimeUnit unit) {
        delayTimer.newTimeout(timeout -> runnable.run(), delay, unit);
    }

    public static void submitToSingleThreadExecutor(Runnable runnable) {
        singleThreadExecutor.submit(runnable);
    }

    public static void submitCallbackTask(String name, Runnable callbackTask) {
        try {
            callbackExecutor.submit(() -> {
                try {
                    callbackTask.run();
                } catch (Exception e) {
                    logger.error("callback run error, name = {}", name);
                }
            });
        } catch (Exception e) {
            ErrorLogCollector.collect(ExecutorUtils.class, "submit callback task error, name = " + name, e);
        }
    }
}
