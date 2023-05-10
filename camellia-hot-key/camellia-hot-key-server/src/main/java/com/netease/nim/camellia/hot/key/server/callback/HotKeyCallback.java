package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.common.model.HotKey;

/**
 * 自定义接口
 * Created by caojiajun on 2023/5/10
 */
public interface HotKeyCallback {

    /**
     * 热key回调接口
     * @param hotKey 热key
     */
    void newHotKey(HotKey hotKey);
}
