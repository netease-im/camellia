package com.netease.nim.camellia.hot.key.sdk.conf;

import com.netease.nim.camellia.core.discovery.CamelliaDiscoveryFactory;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.sdk.collect.CollectorType;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeySdkConfig {


    private String serviceName;
    private CamelliaDiscoveryFactory discoveryFactory;
    private long pushIntervalMillis = HotKeyConstants.Client.pushIntervalMillis;
    private int pushBatch = HotKeyConstants.Client.pushBatch;
    private int capacity = HotKeyConstants.Client.capacity;
    private CollectorType collectorType = CollectorType.Caffeine;
    private boolean async = false;//是否是异步的
    private int asyncQueueCapacity = HotKeyConstants.Client.asyncQueueCapacity;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public CamelliaDiscoveryFactory getDiscoveryFactory() {
        return discoveryFactory;
    }

    public void setDiscoveryFactory(CamelliaDiscoveryFactory discoveryFactory) {
        this.discoveryFactory = discoveryFactory;
    }

    public long getPushIntervalMillis() {
        return pushIntervalMillis;
    }

    public void setPushIntervalMillis(long pushIntervalMillis) {
        this.pushIntervalMillis = pushIntervalMillis;
    }

    public int getPushBatch() {
        return pushBatch;
    }

    public void setPushBatch(int pushBatch) {
        this.pushBatch = pushBatch;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public CollectorType getCollectorType() {
        return collectorType;
    }

    public void setCollectorType(CollectorType collectorType) {
        this.collectorType = collectorType;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public int getAsyncQueueCapacity() {
        return asyncQueueCapacity;
    }

    public void setAsyncQueueCapacity(int asyncQueueCapacity) {
        this.asyncQueueCapacity = asyncQueueCapacity;
    }
}
