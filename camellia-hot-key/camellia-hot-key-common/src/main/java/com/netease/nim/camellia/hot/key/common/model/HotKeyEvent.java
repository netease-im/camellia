package com.netease.nim.camellia.hot.key.common.model;

/**
 * Created by caojiajun on 2023/5/6
 */
public class HotKeyEvent {

    private final String namespace;
    private final KeyAction keyAction;//事件类型
    private final String key;
    private final Long expireMillis;

    public HotKeyEvent(String namespace, KeyAction keyAction, String key, Long expireMillis) {
        this.namespace = namespace;
        this.keyAction = keyAction;
        this.key = key;
        this.expireMillis = expireMillis;
    }

    public String getNamespace() {
        return namespace;
    }

    public KeyAction getKeyAction() {
        return keyAction;
    }

    public String getKey() {
        return key;
    }

    public Long getExpireMillis() {
        return expireMillis;
    }
}
