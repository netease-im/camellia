package com.netease.nim.camellia.hot.key.sdk;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeyCacheSdk implements ICamelliaHotKeyCacheSdk {

    private final CamelliaHotKeySdk sdk;
    private final CamelliaHotKeyCacheSdkConfig config;

    public CamelliaHotKeyCacheSdk(CamelliaHotKeySdk sdk, CamelliaHotKeyCacheSdkConfig config) {
        this.sdk = sdk;
        this.config = config;
    }

    @Override
    public <T> T getValue(String namespace, String key, ValueLoader<T> loader, IValueLoaderLock loaderLock) {
        return null;
    }

    @Override
    public void keyUpdate(String namespace, String key) {

    }

    @Override
    public void keyDelete(String namespace, String key) {

    }
}
