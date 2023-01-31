package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import com.netease.nim.camellia.tools.utils.SysUtils;

/**
 * Created by caojiajun on 2023/1/31
 */
public class CamelliaLinearInitializationExecutorConfig<K, T> {

    private String name;
    private CamelliaLinearInitializationExecutor.Initializer<K, T> initializer;
    private CamelliaDynamicExecutor executor;
    private DynamicValueGetter<Integer> pendingQueueSize = () -> 100000;
    private int clearScheduleIntervalSeconds = 60;
    private DynamicValueGetter<Integer> clearExpireSeconds = () -> 60;

    public CamelliaLinearInitializationExecutorConfig(String name, CamelliaLinearInitializationExecutor.Initializer<K, T> initializer) {
        this.name = name;
        this.initializer = initializer;
        this.executor = new CamelliaDynamicExecutor("liner-initialization-" + name, SysUtils.getCpuNum(), 10000);
    }

    public CamelliaLinearInitializationExecutorConfig(String name, CamelliaLinearInitializationExecutor.Initializer<K, T> initializer,
                                                      DynamicValueGetter<Integer> pendingQueueSize) {
        this.name = name;
        this.initializer = initializer;
        this.executor = new CamelliaDynamicExecutor("liner-initialization-" + name, SysUtils.getCpuNum(), 10000);
        this.pendingQueueSize = pendingQueueSize;
    }

    public CamelliaLinearInitializationExecutorConfig(String name, CamelliaLinearInitializationExecutor.Initializer<K, T> initializer, CamelliaDynamicExecutor executor,
                                                      DynamicValueGetter<Integer> pendingQueueSize, int clearScheduleIntervalSeconds, DynamicValueGetter<Integer> clearExpireSeconds) {
        this.name = name;
        this.initializer = initializer;
        this.executor = executor;
        this.pendingQueueSize = pendingQueueSize;
        this.clearScheduleIntervalSeconds = clearScheduleIntervalSeconds;
        this.clearExpireSeconds = clearExpireSeconds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CamelliaLinearInitializationExecutor.Initializer<K, T> getInitializer() {
        return initializer;
    }

    public void setInitializer(CamelliaLinearInitializationExecutor.Initializer<K, T> initializer) {
        this.initializer = initializer;
    }

    public CamelliaDynamicExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(CamelliaDynamicExecutor executor) {
        this.executor = executor;
    }

    public DynamicValueGetter<Integer> getPendingQueueSize() {
        return pendingQueueSize;
    }

    public void setPendingQueueSize(DynamicValueGetter<Integer> pendingQueueSize) {
        this.pendingQueueSize = pendingQueueSize;
    }

    public int getClearScheduleIntervalSeconds() {
        return clearScheduleIntervalSeconds;
    }

    public void setClearScheduleIntervalSeconds(int clearScheduleIntervalSeconds) {
        this.clearScheduleIntervalSeconds = clearScheduleIntervalSeconds;
    }

    public DynamicValueGetter<Integer> getClearExpireSeconds() {
        return clearExpireSeconds;
    }

    public void setClearExpireSeconds(DynamicValueGetter<Integer> clearExpireSeconds) {
        this.clearExpireSeconds = clearExpireSeconds;
    }
}
