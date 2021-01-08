package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.util.LockMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class HotKeyHunterManager {

    private final ConcurrentHashMap<String, HotKeyHunter> map = new ConcurrentHashMap<>();
    private final HotKeyHunter hotKeyHunter;
    private final LockMap lockMap = new LockMap();

    private final CommandHotKeyMonitorConfig commandHotKeyMonitorConfig;

    public HotKeyHunterManager(CommandHotKeyMonitorConfig commandHotKeyMonitorConfig) {
        this.commandHotKeyMonitorConfig = commandHotKeyMonitorConfig;
        this.hotKeyHunter = new HotKeyHunter(new CommandContext(null, null, null),
                commandHotKeyMonitorConfig.getHotKeyConfig(), commandHotKeyMonitorConfig.getCallback());
    }

    public HotKeyHunter get(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return hotKeyHunter;
        } else {
            String key = bid + "|" + bgroup;
            HotKeyHunter hotKeyHunter = map.get(key);
            if (hotKeyHunter == null) {
                synchronized (lockMap.getLockObj(key)) {
                    hotKeyHunter = map.get(key);
                    if (hotKeyHunter == null) {
                        hotKeyHunter = new HotKeyHunter(new CommandContext(bid, bgroup, null),
                                commandHotKeyMonitorConfig.getHotKeyConfig(), commandHotKeyMonitorConfig.getCallback());
                        map.put(key, hotKeyHunter);
                    }
                }
            }
            return hotKeyHunter;
        }
    }
}
