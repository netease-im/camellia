package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerDiscovery;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeySdkConfig {

    private HotKeyServerDiscovery discovery;
    private long pushIntervalMillis = HotKeyConstants.Client.pushIntervalMillis;
    private int pushBatch = HotKeyConstants.Client.pushBatch;

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
}
