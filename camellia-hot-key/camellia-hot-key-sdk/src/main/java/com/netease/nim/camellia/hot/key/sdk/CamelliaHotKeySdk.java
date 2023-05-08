package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeySdk implements ICamelliaHotKeySdk {

    private final CamelliaHotKeySdkConfig config;

    public CamelliaHotKeySdk(CamelliaHotKeySdkConfig config) {
        this.config = config;
    }

    @Override
    public void push(String namespace, String key, KeyAction keyAction) {

    }

    @Override
    public void addListener(String namespace, CamelliaHotKeyListener listener) {

    }
}
