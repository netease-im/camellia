package com.netease.nim.camellia.hot.key.sdk;

/**
 * Created by caojiajun on 2023/5/7
 */
public interface ICamelliaHotKeyMonitorSdk {

    /**
     * 推送一个key用于统计和检测热key
     * @param namespace namespace
     * @param key key
     */
    void push(String namespace, String key);

}
