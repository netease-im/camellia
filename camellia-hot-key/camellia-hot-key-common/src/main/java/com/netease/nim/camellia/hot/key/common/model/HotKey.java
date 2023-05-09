package com.netease.nim.camellia.hot.key.common.model;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKey {

    private String namespace;
    private String key;
    private KeyAction action;
    private Long expireMillis;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

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

    public Long getExpireMillis() {
        return expireMillis;
    }

    public void setExpireMillis(Long expireMillis) {
        this.expireMillis = expireMillis;
    }
}
