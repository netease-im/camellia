package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicConfig;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;

/**
 * Created by caojiajun on 2023/1/4
 */
public class CamelliaHashedExecutorConfig {

    private static final String PREFIX = "camellia.hashed.executor.config";

    private String name;
    private int poolSize = CamelliaHashedExecutor.defaultPoolSize;
    private DynamicValueGetter<Integer> dynamicQueueSize = () -> CamelliaHashedExecutor.defaultQueueSize;
    private DynamicValueGetter<CamelliaHashedExecutor.RejectedExecutionHandler> rejectedExecutionHandler = () -> CamelliaHashedExecutor.defaultRejectedPolicy;

    public CamelliaHashedExecutorConfig(String name) {
        this.name = name;
    }

    public CamelliaHashedExecutorConfig(String name, int poolSize) {
        this.name = name;
        this.poolSize = poolSize;
    }

    public CamelliaHashedExecutorConfig(String name, int poolSize, DynamicValueGetter<Integer> dynamicQueueSize,
                                        DynamicValueGetter<CamelliaHashedExecutor.RejectedExecutionHandler> rejectedExecutionHandler) {
        this.name = name;
        this.poolSize = poolSize;
        this.dynamicQueueSize = dynamicQueueSize;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    public CamelliaHashedExecutorConfig(String name, int poolSize, DynamicConfig config, int queueSize) {
        this.name = name;
        this.poolSize = poolSize;
        this.dynamicQueueSize = DynamicConfig.wrapper(config, PREFIX + "." + name + ".queueSize", queueSize);
    }

    public CamelliaHashedExecutorConfig(String name, int poolSize, DynamicConfig config) {
        this.name = name;
        this.poolSize = poolSize;
        this.dynamicQueueSize = DynamicConfig.wrapper(config, PREFIX + "." + name + ".queueSize", CamelliaHashedExecutor.defaultQueueSize);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public DynamicValueGetter<Integer> getDynamicQueueSize() {
        return dynamicQueueSize;
    }

    public void setDynamicQueueSize(DynamicValueGetter<Integer> dynamicQueueSize) {
        this.dynamicQueueSize = dynamicQueueSize;
    }

    public DynamicValueGetter<CamelliaHashedExecutor.RejectedExecutionHandler> getRejectedExecutionHandler() {
        return rejectedExecutionHandler;
    }

    public void setRejectedExecutionHandler(DynamicValueGetter<CamelliaHashedExecutor.RejectedExecutionHandler> rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }
}
