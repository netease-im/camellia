package com.netease.nim.camellia.redis.proxy.hotkey.monitor.plugin;

import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.ICamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyMonitorSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;

/**
 * 工厂，返回全局唯一CamelliaHotKeyCacheSdk
 */
public class CamelliaHotKeyMonitorSdkFactory {

    private static volatile ICamelliaHotKeyMonitorSdk monitorSdk;

    private static void init(HotKeyMonitorConfig hotKeyMonitorConfig) {
        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        config.setDiscovery(hotKeyMonitorConfig.getDiscovery());

        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);
        CamelliaHotKeyMonitorSdkConfig camelliaHotKeyMonitorSdkConfig = new CamelliaHotKeyMonitorSdkConfig();
        monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, camelliaHotKeyMonitorSdkConfig);
    }

    /**
     * 获得全局唯一的sdk
     * <p>Get the globally unique sdk
     */
    public static ICamelliaHotKeyMonitorSdk getSdk(HotKeyMonitorConfig hotKeyMonitorConfig) {
        if (monitorSdk == null) {
            synchronized (CamelliaHotKeyMonitorSdkFactory.class) {
                if (monitorSdk == null) {
                    init(hotKeyMonitorConfig);
                }
            }
        }
        return monitorSdk;
    }

}
