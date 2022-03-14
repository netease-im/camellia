package com.netease.nim.camellia.redis.proxy.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.CommandHotKeyCacheConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheStats;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/12/1
 */
public class HotKeyCacheMonitor {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyMonitor.class);

    private static ConcurrentHashMap<String, HotKeyCacheStatsBean> statsMap = new ConcurrentHashMap<>();

    public static void init(int seconds) {
        ExecutorUtils.scheduleAtFixedRate(HotKeyCacheMonitor::calc, seconds, seconds, TimeUnit.SECONDS);
    }

    public static void hotKeyCache(CommandContext commandContext, HotKeyCacheStats hotKeyCacheStats,
                                   CommandHotKeyCacheConfig commandHotKeyCacheConfig) {
        try {
            String bid = commandContext.getBid() == null ? "default" : String.valueOf(commandContext.getBid());
            String bgroup = commandContext.getBgroup() == null ? "default" : commandContext.getBgroup();
            for (HotKeyCacheStats.Stats stats : hotKeyCacheStats.getStatsList()) {
                String keyStr = Utils.bytesToString(stats.getKey());
                String uniqueKey = bid + "|" + bgroup + "|" + keyStr;
                HotKeyCacheStatsBean statsBean = CamelliaMapUtils.computeIfAbsent(statsMap, uniqueKey, k -> new HotKeyCacheStatsBean());
                statsBean.bid = bid;
                statsBean.bgroup = bgroup;
                statsBean.key = keyStr;
                statsBean.checkMillis = commandHotKeyCacheConfig.getCounterCheckMillis();
                statsBean.checkThreshold = commandHotKeyCacheConfig.getCounterCheckThreshold();
                statsBean.hitCount.addAndGet(stats.getHitCount());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static JSONObject monitorJson = new JSONObject();
    private static void calc() {
        try {
            JSONObject json = new JSONObject();
            if (statsMap.isEmpty()) {
                monitorJson = json;
                return;
            }
            ConcurrentHashMap<String, HotKeyCacheStatsBean> statsMap = HotKeyCacheMonitor.statsMap;
            HotKeyCacheMonitor.statsMap = new ConcurrentHashMap<>();
            JSONArray hotKeyCacheStatsJsonArray = new JSONArray();
            for (HotKeyCacheStatsBean statsBean : statsMap.values()) {
                JSONObject hotKeyCacheStatsJson = new JSONObject();
                hotKeyCacheStatsJson.put("bid", statsBean.bid);
                hotKeyCacheStatsJson.put("bgroup", statsBean.bgroup);
                hotKeyCacheStatsJson.put("key", statsBean.key);
                hotKeyCacheStatsJson.put("hitCount", statsBean.hitCount.get());
                hotKeyCacheStatsJson.put("checkMillis", statsBean.checkMillis);
                hotKeyCacheStatsJson.put("checkThreshold", statsBean.checkThreshold);
                hotKeyCacheStatsJsonArray.add(hotKeyCacheStatsJson);
            }
            json.put("hotKeyCacheStats", hotKeyCacheStatsJsonArray);
            monitorJson = json;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static JSONObject getHotKeyCacheStatsJson() {
        return monitorJson;
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
