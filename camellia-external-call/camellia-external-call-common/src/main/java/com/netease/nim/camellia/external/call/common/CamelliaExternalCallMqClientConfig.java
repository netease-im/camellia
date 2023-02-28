package com.netease.nim.camellia.external.call.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by caojiajun on 2023/2/27
 */
public class CamelliaExternalCallMqClientConfig {

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private ScheduledExecutorService scheduledExecutor;
    private int reportIntervalSeconds = 5;

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    public int getReportIntervalSeconds() {
        return reportIntervalSeconds;
    }

    public void setReportIntervalSeconds(int reportIntervalSeconds) {
        this.reportIntervalSeconds = reportIntervalSeconds;
    }
}
