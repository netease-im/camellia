package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.monitor.model.HotKeyStats;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/12/1
 */
public class HotKeyMonitor {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyMonitor.class);
    private static ConcurrentHashMap<String, HotKeyInfo> statsMap = new ConcurrentHashMap<>();

    public static void hotKey(IdentityInfo identityInfo, List<com.netease.nim.camellia.redis.proxy.plugin.hotkey.HotKeyInfo> hotKeys, long checkMillis, long checkThreshold) {
        try {
            String bid = identityInfo.bid() == null ? "default" : String.valueOf(identityInfo.bid());
            String bgroup = identityInfo.bgroup() == null ? "default" : identityInfo.bgroup();
            for (com.netease.nim.camellia.redis.proxy.plugin.hotkey.HotKeyInfo hotKey : hotKeys) {
                String key = Utils.bytesToString(hotKey.getKey());
                String uniqueKey = bid + "|" + bgroup + "|" + key;
                HotKeyInfo hotKeyStats = CamelliaMapUtils.computeIfAbsent(statsMap, uniqueKey, k -> new HotKeyInfo());
                hotKeyStats.bid = bid;
                hotKeyStats.bgroup = bgroup;
                hotKeyStats.key = key;
                hotKeyStats.count.addAndGet(hotKey.getCount());
                hotKeyStats.times.incrementAndGet();
                if (hotKeyStats.max < hotKey.getCount()) {
                    hotKeyStats.max = hotKey.getCount();
                }
                hotKeyStats.checkMillis = checkMillis;
                hotKeyStats.checkThreshold = checkThreshold;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static List<HotKeyStats> collect() {
        if (statsMap.isEmpty()) return new ArrayList<>();
        ConcurrentHashMap<String, HotKeyInfo> statsMap = HotKeyMonitor.statsMap;
        HotKeyMonitor.statsMap = new ConcurrentHashMap<>();
        List<HotKeyStats> list = new ArrayList<>();
        for (HotKeyInfo info : statsMap.values()) {
            HotKeyStats hotKeyStats = new HotKeyStats();
            hotKeyStats.setBid(info.bid);
            hotKeyStats.setBgroup(info.bgroup);
            hotKeyStats.setKey(info.key);
            hotKeyStats.setCount(info.count.get());
            hotKeyStats.setTimes(info.times.get());
            hotKeyStats.setAvg(info.count.get() * 1.0 / info.times.get());
            hotKeyStats.setMax(info.max);
            hotKeyStats.setCheckMillis(info.checkMillis);
            hotKeyStats.setCheckThreshold(info.checkThreshold);
            list.add(hotKeyStats);
        }
        return list;
    }

    private static class HotKeyInfo {
        String bid;
        String bgroup;
        String key;
        AtomicLong count = new AtomicLong();
        AtomicLong times = new AtomicLong();
        long max;
        long checkMillis;
        long checkThreshold;
    }
}
