package com.netease.nim.camellia.console.constant;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public enum ActionRole {
    NORMAL("Normal"),
    ADMIN("Admin");
    private String user;

    ActionRole(String user) {
        this.user = user;
    }
}
