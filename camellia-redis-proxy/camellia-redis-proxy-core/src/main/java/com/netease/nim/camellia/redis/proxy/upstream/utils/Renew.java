package com.netease.nim.camellia.redis.proxy.upstream.utils;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.upstream.proxies.AbstractRedisProxiesClient;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/8/11
 */
public class Renew {

    private static final ThreadPoolExecutor renewExecutor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(1024), new DefaultThreadFactory("renew-executor"));

    private final Runnable renewTask;
    private final AtomicBoolean renewLock = new AtomicBoolean(false);
    private final Resource resource;
    private final ScheduledFuture<?> scheduledFuture;
    private long lastRenewTimestamp = 0L;

    public Renew(Resource resource, Runnable renewTask, int intervalSeconds) {
        this.resource = resource;
        this.renewTask = renewTask;
        this.scheduledFuture = ExecutorUtils.scheduleAtFixedRate(this::renew, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void renew() {
        try {
            //限制1s内最多renew一次
            if (TimeCache.currentMillis - lastRenewTimestamp < 1000) {
                return;
            }
            if (renewLock.compareAndSet(false, true)) {
                renewExecutor.submit(() -> {
                    try {
                        renewTask.run();
                        lastRenewTimestamp = TimeCache.currentMillis;
                    } catch (Exception e) {
                        ErrorLogCollector.collect(AbstractRedisProxiesClient.class, "renew error, url = " + PasswordMaskUtils.maskResource(resource), e);
                    } finally {
                        renewLock.set(false);
                    }
                });
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(AbstractRedisProxiesClient.class, "submit renew task error, url = " + PasswordMaskUtils.maskResource(resource), e);
        }
    }

    public void close() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }
}
