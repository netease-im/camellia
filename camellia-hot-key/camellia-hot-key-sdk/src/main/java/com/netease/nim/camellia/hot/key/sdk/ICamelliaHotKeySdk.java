package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;

/**
 * Created by caojiajun on 2023/5/6
 */
public interface ICamelliaHotKeySdk {

    /**
     * 推送一个key的动作给server
     * @param namespace namespace
     * @param key key
     * @param keyAction 动作
     */
    void push(String namespace, String key, KeyAction keyAction);

    /**
     * 获取热key配置
     * @param namespace namespace
     * @return 热key配置
     */
    HotKeyConfig getHotKeyConfig(String namespace);

    /**
     * 增加一个key的事件监听
     * @param namespace namespace
     * @param listener 监听器
     */
    void addListener(String namespace, CamelliaHotKeyListener listener);

    /**
     * 增加一个热key配置的事件监听
     * @param namespace namespace
     * @param listener 监听器
     */
    void addListener(String namespace, CamelliaHotKeyConfigListener listener);

}
