package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class HotKeyHunterManager {

    private final ConcurrentHashMap<String, HotKeyHunter> map = new ConcurrentHashMap<>();
    private volatile HotKeyHunter hotKeyHunter;

    private final CommandHotKeyMonitorConfig commandHotKeyMonitorConfig;

    public HotKeyHunterManager(CommandHotKeyMonitorConfig commandHotKeyMonitorConfig) {
        this.commandHotKeyMonitorConfig = commandHotKeyMonitorConfig;
    }

    public HotKeyHunter get(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            if (hotKeyHunter == null) {
                synchronized (HotKeyHunterManager.class) {
                    if (hotKeyHunter == null) {
                        hotKeyHunter = new HotKeyHunter(commandHotKeyMonitorConfig.getHotKeyConfig(), commandHotKeyMonitorConfig.getCallback());
                    }
                }
            }
            return hotKeyHunter;
        } else {
            String key = bid + "|" + bgroup;
            HotKeyHunter hotKeyHunter = map.get(key);
            if (hotKeyHunter == null) {
                synchronized (HotKeyHunterManager.class) {
                    hotKeyHunter = map.get(key);
                    if (hotKeyHunter == null) {
                        hotKeyHunter = new HotKeyHunter(commandHotKeyMonitorConfig.getHotKeyConfig(), commandHotKeyMonitorConfig.getCallback());
                        map.put(key, hotKeyHunter);
                    }
                }
            }
            return hotKeyHunter;
        }
    }
}
