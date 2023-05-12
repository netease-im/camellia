package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyMonitorSdkConfig;

/**
 * Created by caojiajun on 2023/5/7
 */
public interface ICamelliaHotKeyMonitorSdk {

    /**
     * 推送一个key用于统计和检测热key
     * @param namespace namespace
     * @param key key
     * @return Result 结果
     */
    Result push(String namespace, String key);

    /**
     * 获取当配置
     * @return 配置
     */
    CamelliaHotKeyMonitorSdkConfig getConfig();
}
