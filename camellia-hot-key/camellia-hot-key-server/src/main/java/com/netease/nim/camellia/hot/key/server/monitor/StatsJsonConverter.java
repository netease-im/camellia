package com.netease.nim.camellia.hot.key.server.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyInfo;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/11
 */
public class StatsJsonConverter {

    public static String converter(HotKeyServerStats serverStats) {
        if (serverStats == null) return new JSONObject().toJSONString();
        JSONObject monitorJson = new JSONObject();
        QueueStats queueStats = serverStats.getQueueStats();

        JSONArray infoJsonArray = new JSONArray();
        JSONObject info = new JSONObject();
        info.put("workThread", queueStats.getQueueNum());
        info.put("applicationName", serverStats.getApplicationName());
        info.put("monitorIntervalSeconds", serverStats.getMonitorIntervalSeconds());
        infoJsonArray.add(info);

        monitorJson.put("info", infoJsonArray);

        JSONArray queueStatsJsonArray = new JSONArray();
        for (QueueStats.Stats stats : queueStats.getStatsList()) {
            JSONObject json = new JSONObject();
            json.put("id", String.valueOf(stats.getId()));
            json.put("pendingSize", stats.getPendingSize());
            queueStatsJsonArray.add(json);
        }
        monitorJson.put("queueStats", queueStatsJsonArray);

        TrafficStats trafficStats = serverStats.getTrafficStats();
        info.put("count", trafficStats.getTotal());

        JSONArray trafficTotalStatsJsonArray = new JSONArray();
        JSONObject total = new JSONObject();
        total.put("count", trafficStats.getTotal());
        total.put("qps", trafficStats.getTotal() * 1.0 / serverStats.getMonitorIntervalSeconds());
        trafficTotalStatsJsonArray.add(total);
        monitorJson.put("trafficTotalStats", trafficTotalStatsJsonArray);

        JSONArray trafficStatsJsonArray = new JSONArray();
        for (TrafficStats.Stats stats : trafficStats.getStatsList()) {
            JSONObject json = new JSONObject();
            json.put("namespace", stats.getNamespace());
            json.put("type", stats.getType());
            json.put("count", stats.getCount());
            trafficStatsJsonArray.add(json);
        }
        monitorJson.put("trafficStats", trafficStatsJsonArray);

        JSONArray hotKeyInfoStatsJsonArray = new JSONArray();
        List<HotKeyInfo> hotKeyInfoList = serverStats.getHotKeyInfoList();
        for (HotKeyInfo hotKeyInfo : hotKeyInfoList) {
            JSONObject json = new JSONObject();
            json.put("namespace", hotKeyInfo.getNamespace());
            json.put("key", hotKeyInfo.getKey());
            json.put("action", hotKeyInfo.getAction());
            json.put("rule", JSONObject.toJSONString(hotKeyInfo.getRule()));
            json.put("count", hotKeyInfo.getCount());
            hotKeyInfoStatsJsonArray.add(json);
        }
        monitorJson.put("hotKeyInfoStats", hotKeyInfoStatsJsonArray);

        return monitorJson.toJSONString();
    }
}
