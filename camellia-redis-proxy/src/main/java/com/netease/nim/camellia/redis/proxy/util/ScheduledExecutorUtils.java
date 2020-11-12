package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.SysUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2020/11/11
 */
public class ScheduledExecutorUtils {

    private static final ScheduledExecutorService service = Executors.newScheduledThreadPool(SysUtils.getCpuNum(), new CamelliaThreadFactory("camellia-schedule"));

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                         long initialDelay,
                                                         long period,
                                                         TimeUnit unit) {
        return service.scheduleAtFixedRate(command, initialDelay, period, unit);
    }
}
