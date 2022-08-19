package com.netease.nim.camellia.console.constant;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public enum ActionType {
    READ("R"),
    WRITE("W");
    private final String action;

    ActionType(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return action;
    }
}
