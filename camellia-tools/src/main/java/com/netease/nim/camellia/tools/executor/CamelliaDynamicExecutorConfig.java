package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicConfig;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;

import java.util.concurrent.*;

/**
 * 可以动态修改线程池参数的线程池
 * Created by caojiajun on 2023/1/3
 */
public class CamelliaDynamicExecutorConfig {

    private static final String PREFIX = "camellia.dynamic.executor.config";

    private String name;
    private DynamicValueGetter<Integer> corePoolSize;
    private DynamicValueGetter<Integer> maxPoolSize;
    private DynamicValueGetter<Long> keepAliveTime = () -> 0L;
    private DynamicValueGetter<TimeUnit> unit = () -> TimeUnit.SECONDS;
    private DynamicValueGetter<Integer> queueSize = () -> Integer.MAX_VALUE;
    private DynamicValueGetter<RejectedExecutionHandler> rejectedExecutionHandler = ThreadPoolExecutor.AbortPolicy::new;

    public CamelliaDynamicExecutorConfig(String name, DynamicValueGetter<Integer> corePoolSize, DynamicValueGetter<Integer> maxPoolSize) {
        this.name = name;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
    }

    public CamelliaDynamicExecutorConfig(String name, DynamicValueGetter<Integer> corePoolSize,
                                         DynamicValueGetter<Integer> maxPoolSize, DynamicValueGetter<Integer> queueSize) {
        this.name = name;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueSize = queueSize;
    }

    public CamelliaDynamicExecutorConfig(String name, DynamicConfig dynamicConfig, int corePoolSize, int maxPoolSize) {
        this.name = name;
        this.corePoolSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".corePoolSize", corePoolSize);
        this.maxPoolSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".maxPoolSize", maxPoolSize);
        this.queueSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".queueSize", Integer.MAX_VALUE);
    }

    public CamelliaDynamicExecutorConfig(String name, DynamicConfig dynamicConfig, int corePoolSize, int maxPoolSize, int queueSize) {
        this.name = name;
        this.corePoolSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".corePoolSize", corePoolSize);
        this.maxPoolSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".maxPoolSize", maxPoolSize);
        this.queueSize = DynamicConfig.wrapper(dynamicConfig, PREFIX + "." + name + ".queueSize", queueSize);
    }

    public CamelliaDynamicExecutorConfig(String name, DynamicValueGetter<Integer> corePoolSize, DynamicValueGetter<Integer> maxPoolSize,
                                         DynamicValueGetter<Long> keepAliveTime, DynamicValueGetter<TimeUnit> unit,
                                         DynamicValueGetter<Integer> queueSize, DynamicValueGetter<RejectedExecutionHandler> rejectedExecutionHandler) {
        this.name = name;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.queueSize = queueSize;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DynamicValueGetter<Integer> getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(DynamicValueGetter<Integer> corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public DynamicValueGetter<Integer> getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(DynamicValueGetter<Integer> maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public DynamicValueGetter<Long> getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(DynamicValueGetter<Long> keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public DynamicValueGetter<TimeUnit> getUnit() {
        return unit;
    }

    public void setUnit(DynamicValueGetter<TimeUnit> unit) {
        this.unit = unit;
    }

    public DynamicValueGetter<Integer> getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(DynamicValueGetter<Integer> queueSize) {
        this.queueSize = queueSize;
    }

    public DynamicValueGetter<RejectedExecutionHandler> getRejectedExecutionHandler() {
        return rejectedExecutionHandler;
    }

    public void setRejectedExecutionHandler(DynamicValueGetter<RejectedExecutionHandler> rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }
}
