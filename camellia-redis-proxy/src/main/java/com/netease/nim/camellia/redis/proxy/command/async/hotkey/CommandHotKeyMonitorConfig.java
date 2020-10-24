package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandHotKeyMonitorConfig {

    private final HotKeyConfig hotKeyConfig;
    private final HotKeyCallback callback;

    public CommandHotKeyMonitorConfig(HotKeyConfig hotKeyConfig, HotKeyCallback callback) {
        this.hotKeyConfig = hotKeyConfig;
        this.callback = callback;
    }

    public HotKeyConfig getHotKeyConfig() {
        return hotKeyConfig;
    }

    public HotKeyCallback getCallback() {
        return callback;
    }
}
