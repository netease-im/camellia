package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.SysUtils;

import java.util.concurrent.*;

/**
 * Created by caojiajun on 2023/5/7
 */
public class CamelliaHotKeyMonitorSdkConfig {

    private static final ScheduledExecutorService defaultScheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-hot-key-monitor-scheduler"));

    private static final ThreadPoolExecutor defaultExecutor = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuHalfNum(), 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000),
            new CamelliaThreadFactory("camellia-hot-key-monitor-executor"), new ThreadPoolExecutor.CallerRunsPolicy());

    private ThreadPoolExecutor executor = defaultExecutor;
    private ScheduledExecutorService scheduler = defaultScheduler;
    private int hotKeyConfigReloadIntervalSeconds = HotKeyConstants.Client.hotKeyConfigReloadIntervalSeconds;

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    public int getHotKeyConfigReloadIntervalSeconds() {
        return hotKeyConfigReloadIntervalSeconds;
    }

    public void setHotKeyConfigReloadIntervalSeconds(int hotKeyConfigReloadIntervalSeconds) {
        this.hotKeyConfigReloadIntervalSeconds = hotKeyConfigReloadIntervalSeconds;
    }
}
