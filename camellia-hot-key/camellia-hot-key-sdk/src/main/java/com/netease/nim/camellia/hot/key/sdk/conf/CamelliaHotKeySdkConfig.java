package com.netease.nim.camellia.hot.key.sdk.conf;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.sdk.collect.CollectorType;
import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeySdkConfig {

    private HotKeyServerDiscovery discovery;
    private long pushIntervalMillis = HotKeyConstants.Client.pushIntervalMillis;
    private int pushBatch = HotKeyConstants.Client.pushBatch;
    private int capacity = HotKeyConstants.Client.capacity;
    private CollectorType collectorType = CollectorType.Caffeine;
    private boolean async = false;//是否是异步的
    private int asyncQueueCapacity = HotKeyConstants.Client.asyncQueueCapacity;

    public HotKeyServerDiscovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(HotKeyServerDiscovery discovery) {
        this.discovery = discovery;
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
