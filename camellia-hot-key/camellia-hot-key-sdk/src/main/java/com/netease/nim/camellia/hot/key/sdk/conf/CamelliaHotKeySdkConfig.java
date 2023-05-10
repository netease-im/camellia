package com.netease.nim.camellia.hot.key.sdk.conf;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerDiscovery;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeySdkConfig {

    private HotKeyServerDiscovery discovery;
    private long pushIntervalMillis = HotKeyConstants.Client.pushIntervalMillis;
    private int pushBatch = HotKeyConstants.Client.pushBatch;
    private int capacity = HotKeyConstants.Client.capacity;

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
}
