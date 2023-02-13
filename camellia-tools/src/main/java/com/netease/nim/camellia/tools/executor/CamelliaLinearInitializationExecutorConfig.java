package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import com.netease.nim.camellia.tools.utils.SysUtils;

/**
 * Created by caojiajun on 2023/1/31
 */
public class CamelliaLinearInitializationExecutorConfig<K, T> {

    private String name;
    private CamelliaLinearInitializationExecutor.Initializer<K, T> initializer;
    private DynamicValueGetter<Integer> pendingQueueSize = () -> 100000;
    private int poolSize = SysUtils.getCpuNum();

    public CamelliaLinearInitializationExecutorConfig(String name, CamelliaLinearInitializationExecutor.Initializer<K, T> initializer) {
        this.name = name;
        this.initializer = initializer;
    }

    public CamelliaLinearInitializationExecutorConfig(String name, CamelliaLinearInitializationExecutor.Initializer<K, T> initializer,
                                                      DynamicValueGetter<Integer> pendingQueueSize) {
        this.name = name;
        this.initializer = initializer;
        this.pendingQueueSize = pendingQueueSize;
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

    public DynamicValueGetter<Integer> getPendingQueueSize() {
        return pendingQueueSize;
    }

    public void setPendingQueueSize(DynamicValueGetter<Integer> pendingQueueSize) {
        this.pendingQueueSize = pendingQueueSize;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
}
