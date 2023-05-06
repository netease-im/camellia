package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.model.HotKeyEvent;

/**
 * Created by caojiajun on 2023/5/6
 */
public interface CamelliaHotKeyListener {

    /**
     * 热key事件监听回调方法
     * @param event 事件
     */
    void onHotKeyEvent(HotKeyEvent event);
}
