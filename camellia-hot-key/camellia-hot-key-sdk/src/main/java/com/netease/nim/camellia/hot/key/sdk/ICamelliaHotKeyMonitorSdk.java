package com.netease.nim.camellia.hot.key.sdk;

/**
 * Created by caojiajun on 2023/5/7
 */
public interface ICamelliaHotKeyMonitorSdk {

    /**
     * 推送一个key用于统计和健康热key
     * @param key key
     */
    void push(String key);

}
