package com.netease.nim.camellia.hot.key.server.callback;


/**
 * 自定义接口
 * Created by caojiajun on 2023/5/10
 */
public interface HotKeyCallback {

    /**
     * 热key回调接口
     * @param hotKeyInfo 热key信息
     */
    void newHotKey(HotKeyInfo hotKeyInfo);
}
