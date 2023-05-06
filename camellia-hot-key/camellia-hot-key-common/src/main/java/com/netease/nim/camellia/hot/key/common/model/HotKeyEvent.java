package com.netease.nim.camellia.hot.key.common.model;

/**
 * Created by caojiajun on 2023/5/6
 */
public class HotKeyEvent {

    private final HotKeyEventType eventType;//事件类型
    private final String key;

    public HotKeyEvent(HotKeyEventType eventType, String key) {
        this.eventType = eventType;
        this.key = key;
    }

    public HotKeyEventType getEventType() {
        return eventType;
    }

    public String getKey() {
        return key;
    }
}
