package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.util.SysUtils;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2020/11/11
 */
public class ExecutorUtils {

    private static final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(SysUtils.getCpuNum(), new DefaultThreadFactory("camellia-schedule"));
    private static final HashedWheelTimer delayTimer = new HashedWheelTimer(new DefaultThreadFactory("camellia-delay-timer"));

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                         long initialDelay,
                                                         long period,
                                                         TimeUnit unit) {
        return scheduleService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public static Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        return delayTimer.newTimeout(task, delay, unit);
    }
}
