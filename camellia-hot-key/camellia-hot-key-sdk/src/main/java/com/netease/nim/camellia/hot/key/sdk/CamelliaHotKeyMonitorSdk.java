package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;

/**
 * Created by caojiajun on 2023/5/7
 */
public class CamelliaHotKeyMonitorSdk implements ICamelliaHotKeyMonitorSdk {

    private final CamelliaHotKeySdk sdk;
    private CamelliaHotKeyMonitorSdkConfig config;

    public CamelliaHotKeyMonitorSdk(CamelliaHotKeySdk sdk, CamelliaHotKeyMonitorSdkConfig config) {
        this.sdk = sdk;
        this.config = config;
    }

    @Override
    public void push(String key) {
        sdk.push(key, KeyAction.QUERY);
    }
}
