package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.util.SysUtils;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2020/11/11
 */
public class ExecutorUtils {

    private static final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(SysUtils.getCpuNum(), new DefaultThreadFactory("camellia-schedule"));
    private static final ExecutorService asyncService = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(), 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(4096), new DefaultThreadFactory("camellia-async"), new ThreadPoolExecutor.AbortPolicy());

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                         long initialDelay,
                                                         long period,
                                                         TimeUnit unit) {
        return scheduleService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public static void submit(Runnable runnable) {
        asyncService.submit(runnable);
    }

    public static <T> Future<T> submit(Callable<T> callable) {
        return asyncService.submit(callable);
    }
}
