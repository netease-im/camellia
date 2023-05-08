package com.netease.nim.camellia.hot.key.common.model;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyCounter {

    private String key;
    private KeyAction action;
    private long count;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public KeyAction getAction() {
        return action;
    }

    public void setAction(KeyAction action) {
        this.action = action;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
