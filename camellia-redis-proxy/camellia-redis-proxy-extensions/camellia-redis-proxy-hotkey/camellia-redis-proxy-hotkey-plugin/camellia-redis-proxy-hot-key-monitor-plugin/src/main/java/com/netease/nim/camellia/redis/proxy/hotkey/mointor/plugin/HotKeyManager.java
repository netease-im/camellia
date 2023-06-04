package com.netease.nim.camellia.redis.proxy.hotkey.mointor.plugin;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.LockMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 工厂模式，创建单例{@link HotKeyMonitor}
 */
public class HotKeyManager {
    /**
     * 如果不是多租户，就只有一个{@link HotKeyMonitor}
     */
    private final HotKeyMonitor hotKeyMonitor;

    public ConcurrentHashMap<String, HotKeyMonitor> hotKeyContainer = new ConcurrentHashMap<>();

    private final LockMap lockMap = new LockMap();

    private final HotKeyMonitorConfig hotKeyMonitorConfig;

    public HotKeyManager(HotKeyMonitorConfig hotKeyMonitorConfig) {
        this.hotKeyMonitorConfig = hotKeyMonitorConfig;
        hotKeyMonitor = new HotKeyMonitor(new IdentityInfo(null, null), hotKeyMonitorConfig);
    }

    /**
     * DCL
     */
    public HotKeyMonitor getHotKey(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return hotKeyMonitor;
        } else {
            String key = Utils.getCacheKey(bid, bgroup);
            HotKeyMonitor hotkeyMonitor = hotKeyContainer.get(key);
            if (hotkeyMonitor == null) {
                synchronized (lockMap.getLockObj(key)) {
                    hotkeyMonitor = hotKeyContainer.get(key);
                    if (hotkeyMonitor == null) {
                        hotkeyMonitor = new HotKeyMonitor(new IdentityInfo(bid, bgroup), hotKeyMonitorConfig);
                        hotKeyContainer.put(key, hotkeyMonitor);
                    }
                }
            }
            return hotkeyMonitor;
        }
    }
}
