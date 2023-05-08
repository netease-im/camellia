package com.netease.nim.camellia.hot.key.common.model;

/**
 * Created by caojiajun on 2023/5/6
 */
public enum KeyAction {

    QUERY(1),
    UPDATE(2),
    DELETE(3),
    ;

    ;
    private final int value;

    KeyAction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static KeyAction getByValue(int value) {
        for (KeyAction action : KeyAction.values()) {
            if (action.value == value) {
                return action;
            }
        }
        return null;
    }
}
