package com.netease.nim.camellia.config.auth;

/**
 * Created by caojiajun on 2023/3/15
 */
public class EnvContext {

    private static final ThreadLocal<String> userThreadLocal = new ThreadLocal<>();

    public static void setUser(String user) {
        userThreadLocal.set(user);
    }

    public static String getUser() {
        return userThreadLocal.get();
    }
}
