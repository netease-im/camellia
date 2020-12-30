package com.netease.nim.camellia.redis.proxy.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyInfo;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/12/1
 */
public class HotKeyMonitor {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyMonitor.class);
    private static ConcurrentHashMap<String, HotKeyStats> statsMap = new ConcurrentHashMap<>();

    public static void init(int seconds) {
        ExecutorUtils.scheduleAtFixedRate(HotKeyMonitor::calc, seconds, seconds, TimeUnit.SECONDS);
    }

    public static void hotKey(CommandContext commandContext, List<HotKeyInfo> hotKeys, HotKeyConfig hotKeyConfig) {
        try {
            String bid = commandContext.getBid() == null ? "default" : String.valueOf(commandContext.getBid());
            String bgroup = commandContext.getBgroup() == null ? "default" : commandContext.getBgroup();
            for (HotKeyInfo hotKey : hotKeys) {
                String key = Utils.bytesToString(hotKey.getKey());
                String uniqueKey = bid + "|" + bgroup + "|" + key;
                HotKeyStats hotKeyStats = statsMap.computeIfAbsent(uniqueKey, k -> new HotKeyStats());
                hotKeyStats.bid = bid;
                hotKeyStats.bgroup = bgroup;
                hotKeyStats.key = key;
                hotKeyStats.count.addAndGet(hotKey.getCount());
                hotKeyStats.times.incrementAndGet();
                if (hotKeyStats.max < hotKey.getCount()) {
                    hotKeyStats.max = hotKey.getCount();
                }
                hotKeyStats.checkMillis = hotKeyConfig.getCheckMillis();
                hotKeyStats.checkThreshold = hotKeyConfig.getCheckThreshold();
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
            ConcurrentHashMap<String, HotKeyStats> statsMap = HotKeyMonitor.statsMap;
            HotKeyMonitor.statsMap = new ConcurrentHashMap<>();
            JSONArray hotKeyJsonArray = new JSONArray();
            for (HotKeyStats hotKeyStats : statsMap.values()) {
                JSONObject hotKeyJson = new JSONObject();
                hotKeyJson.put("bid", hotKeyStats.bid);
                hotKeyJson.put("bgroup", hotKeyStats.bgroup);
                hotKeyJson.put("key", hotKeyStats.key);
                hotKeyJson.put("count", hotKeyStats.count.get());//total count of callback
                hotKeyJson.put("times", hotKeyStats.times.get());//callback times
                hotKeyJson.put("avg", hotKeyStats.count.get() * 1.0 / hotKeyStats.times.get());//avg count of every callback
                hotKeyJson.put("max", hotKeyStats.max);//max count of every callback
                hotKeyJson.put("checkMillis", hotKeyStats.checkMillis);
                hotKeyJson.put("checkThreshold", hotKeyStats.checkThreshold);
                hotKeyJsonArray.add(hotKeyJson);
            }
            json.put("hotKeyStats", hotKeyJsonArray);
            monitorJson = json;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static JSONObject getHotKeyStatsJson() {
        return monitorJson;
    }

    private static class HotKeyStats {
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
