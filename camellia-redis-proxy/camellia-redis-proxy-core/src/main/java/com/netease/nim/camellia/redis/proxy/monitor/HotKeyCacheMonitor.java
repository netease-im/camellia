package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.monitor.model.HotKeyCacheStats;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotKeyCacheInfo;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.plugin.hotkey.HotKeyMonitor;
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
public class HotKeyCacheMonitor {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyMonitor.class);

    private static ConcurrentHashMap<String, HotKeyCacheStatsBean> statsMap = new ConcurrentHashMap<>();

    public static void hotKeyCache(IdentityInfo identityInfo, HotKeyCacheInfo hotKeyCacheInfo,
                                   long checkMillis, long checkThreshold) {
        try {
            String bid = identityInfo.bid() == null ? "default" : String.valueOf(identityInfo.bid());
            String bgroup = identityInfo.bgroup() == null ? "default" : identityInfo.bgroup();
            for (HotKeyCacheInfo.Stats stats : hotKeyCacheInfo.getStatsList()) {
                String keyStr = Utils.bytesToString(stats.getKey());
                String uniqueKey = bid + "|" + bgroup + "|" + keyStr;
                HotKeyCacheStatsBean statsBean = CamelliaMapUtils.computeIfAbsent(statsMap, uniqueKey, k -> new HotKeyCacheStatsBean());
                statsBean.bid = bid;
                statsBean.bgroup = bgroup;
                statsBean.key = keyStr;
                statsBean.checkMillis = checkMillis;
                statsBean.checkThreshold = checkThreshold;
                statsBean.hitCount.addAndGet(stats.getHitCount());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static List<HotKeyCacheStats> collect() {
        ConcurrentHashMap<String, HotKeyCacheStatsBean> statsMap = HotKeyCacheMonitor.statsMap;
        HotKeyCacheMonitor.statsMap = new ConcurrentHashMap<>();
        List<HotKeyCacheStats> list = new ArrayList<>();
        for (HotKeyCacheStatsBean statsBean : statsMap.values()) {
            HotKeyCacheStats stats = new HotKeyCacheStats();
            stats.setBid(statsBean.bid);
            stats.setBgroup(statsBean.bgroup);
            stats.setKey(statsBean.key);
            stats.setHitCount(statsBean.hitCount.get());
            stats.setCheckMillis(statsBean.checkMillis);
            stats.setCheckThreshold(statsBean.checkThreshold);
            list.add(stats);
        }
        return list;
    }

    private static class HotKeyCacheStatsBean {
        String bid;
        String bgroup;
        String key;
        AtomicLong hitCount = new AtomicLong();
        long checkMillis;
        long checkThreshold;
    }

}
