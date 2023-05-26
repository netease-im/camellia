package com.netease.nim.camellia.hot.key.server.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.server.calculate.TopNStats;
import com.netease.nim.camellia.hot.key.server.calculate.TopNStatsResult;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/11
 */
public class TopNStatsResultJsonConverter {

    public static String converter(TopNStatsResult result) {
        if (result == null) return new JSONObject().toJSONString();
        JSONObject monitorJson = new JSONObject();

        JSONArray infoJsonArray = new JSONArray();
        JSONObject infoJson = new JSONObject();
        infoJson.put("namespace", result.getNamespace());
        infoJson.put("statsTime", result.getTime());
        infoJsonArray.add(infoJson);
        monitorJson.put("info", infoJsonArray);

        JSONArray statsJsonArray = new JSONArray();
        List<TopNStats> topN = result.getTopN();
        for (int i=0; i<topN.size(); i++) {
            TopNStats topNStats = topN.get(i);
            JSONObject json = new JSONObject();
            json.put("id", String.valueOf(i));
            json.put("key", topNStats.getKey());
            json.put("action", topNStats.getAction());
            json.put("total", topNStats.getTotal());
            json.put("maxQps", topNStats.getMaxQps());
            json.put("sourceSet", JSONObject.toJSONString(topNStats.getSourceSet()));
            statsJsonArray.add(json);
        }
        monitorJson.put("stats", statsJsonArray);

        return monitorJson.toJSONString();
    }
}
