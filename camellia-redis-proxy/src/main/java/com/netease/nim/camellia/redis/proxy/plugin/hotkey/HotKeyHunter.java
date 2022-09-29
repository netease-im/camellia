package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
import com.netease.nim.camellia.redis.proxy.util.LRUCounter;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2020/10/20
 */
public class HotKeyHunter {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyHunter.class);

    private final String CALLBACK_NAME;

    private boolean enable;
    private final HotKeyMonitorCallback callback;
    private final LRUCounter counter;
    private final IdentityInfo identityInfo;
    private final long checkMillis;

    private long checkThreshold;
    private int maxHotKeyCount;

    public HotKeyHunter(IdentityInfo identityInfo, HotKeyMonitorCallback callback) {
        this.identityInfo = identityInfo;
        this.enable = true;
        reloadHotKeyConfig();
        ProxyDynamicConf.registerCallback(this::reloadHotKeyConfig);
        this.callback = callback;
        this.CALLBACK_NAME = callback.getClass().getName();
        int checkCacheMaxCapacity = ProxyDynamicConf.getInt("hot.key.monitor.cache.max.capacity",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyMonitorCheckCacheMaxCapacity);
        this.checkMillis = ProxyDynamicConf.getLong("hot.key.monitor.counter.check.millis",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheCounterCheckMillis);
        this.counter = new LRUCounter(checkCacheMaxCapacity,
                checkCacheMaxCapacity, checkMillis);
        ExecutorUtils.scheduleAtFixedRate(this::callback, checkMillis,
                checkMillis, TimeUnit.MILLISECONDS);
        logger.info("HotKeyHunter init success, identityInfo = {}", identityInfo);
    }

    private void reloadHotKeyConfig() {
        Long bid = identityInfo.getBid();
        String bgroup = identityInfo.getBgroup();
        this.maxHotKeyCount = ProxyDynamicConf.getInt("hot.key.monitor.max.hot.key.count",
                bid, bgroup, Constants.Server.hotKeyMonitorMaxHotKeyCount);
        this.checkThreshold = ProxyDynamicConf.getLong("hot.key.monitor.counter.check.threshold",
                bid, bgroup, Constants.Server.hotKeyMonitorCheckThreshold);
        this.enable = ProxyDynamicConf.getBoolean("hot.key.monitor.enable", bid, bgroup, true);
    }

    public void incr(byte[]... keys) {
        if (!enable) return;
        for (byte[] key : keys) {
            incr(key);
        }
    }

    public void incr(List<byte[]> keys) {
        if (!enable) return;
        for (byte[] key : keys) {
            incr(key);
        }
    }

    private void incr(byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        counter.increment(bytesKey);
    }

    private void callback() {
        try {
            TreeSet<LRUCounter.SortedBytesKey> set = counter.getSortedCacheValue(checkThreshold);
            if (set == null || set.isEmpty()) return;
            List<HotKeyInfo> hotKeys = new ArrayList<>(maxHotKeyCount);
            for (LRUCounter.SortedBytesKey sortedBytesKey : set) {
                hotKeys.add(new HotKeyInfo(sortedBytesKey.getKey(), sortedBytesKey.getCount()));
                if (hotKeys.size() >= maxHotKeyCount) {
                    break;
                }
            }
            HotKeyMonitor.hotKey(identityInfo, hotKeys, checkMillis, checkThreshold);
            ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> callback.callback(identityInfo, hotKeys, checkMillis, checkThreshold));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
