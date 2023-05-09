package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.SysUtils;

import java.util.concurrent.*;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeyCacheSdkConfig {

    private static final ScheduledExecutorService defaultScheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-hot-key-cache-scheduler"));

    private static final ThreadPoolExecutor defaultExecutor = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuHalfNum(), 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000),
            new CamelliaThreadFactory("camellia-hot-key-cache-executor"), new ThreadPoolExecutor.CallerRunsPolicy());

    private ThreadPoolExecutor executor = defaultExecutor;
    private ScheduledExecutorService scheduler = defaultScheduler;
    private int hotKeyConfigReloadIntervalSeconds = HotKeyConstants.Client.hotKeyConfigReloadIntervalSeconds;

    private String namespace;
    private int capacity = 10000;
    private boolean cacheNull = true;
    private int loadTryLockRetry = 3;
    private long loadTryLockSleepMs = 1;

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public int getHotKeyConfigReloadIntervalSeconds() {
        return hotKeyConfigReloadIntervalSeconds;
    }

    public void setHotKeyConfigReloadIntervalSeconds(int hotKeyConfigReloadIntervalSeconds) {
        this.hotKeyConfigReloadIntervalSeconds = hotKeyConfigReloadIntervalSeconds;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isCacheNull() {
        return cacheNull;
    }

    public void setCacheNull(boolean cacheNull) {
        this.cacheNull = cacheNull;
    }

    public int getLoadTryLockRetry() {
        return loadTryLockRetry;
    }

    public void setLoadTryLockRetry(int loadTryLockRetry) {
        this.loadTryLockRetry = loadTryLockRetry;
    }

    public long getLoadTryLockSleepMs() {
        return loadTryLockSleepMs;
    }

    public void setLoadTryLockSleepMs(long loadTryLockSleepMs) {
        this.loadTryLockSleepMs = loadTryLockSleepMs;
    }
}
