package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandHotKeyMonitorConfig {

    private final HotKeyConfig hotKeyConfig;
    private final HotKeyMonitorCallback callback;

    public CommandHotKeyMonitorConfig(HotKeyConfig hotKeyConfig, HotKeyMonitorCallback callback) {
        this.hotKeyConfig = hotKeyConfig;
        this.callback = callback;
    }

    public HotKeyConfig getHotKeyConfig() {
        return hotKeyConfig;
    }

    public HotKeyMonitorCallback getCallback() {
        return callback;
    }
}
