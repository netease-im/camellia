package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.core.util.LockMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class HotKeyHunterManager {

    private final ConcurrentHashMap<String, HotKeyHunter> map = new ConcurrentHashMap<>();
    private final HotKeyHunter hotKeyHunter;
    private final LockMap lockMap = new LockMap();

    private final HotKeyMonitorCallback callback;

    public HotKeyHunterManager(HotKeyMonitorCallback callback) {
        this.callback = callback;
        this.hotKeyHunter = new HotKeyHunter(new IdentityInfo(null, null), callback);
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
                        hotKeyHunter = new HotKeyHunter(new IdentityInfo(bid, bgroup), callback);
                        map.put(key, hotKeyHunter);
                    }
                }
            }
            return hotKeyHunter;
        }
    }
}
