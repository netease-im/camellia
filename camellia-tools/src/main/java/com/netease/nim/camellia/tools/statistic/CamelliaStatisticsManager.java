package com.netease.nim.camellia.tools.statistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/8/17
 */
public class CamelliaStatisticsManager {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaStatisticsManager.class);
    private ConcurrentHashMap<String, CamelliaStatistics> map = new ConcurrentHashMap<>();

    public void update(String key, long value) {
        CamelliaStatistics statistics = map.get(key);
        if (statistics == null) {
            statistics = map.computeIfAbsent(key, k -> new CamelliaStatistics());
        }
        statistics.update(value);
    }

    public Map<String, CamelliaStatsData> getStatsDataAndReset() {
        try {
            ConcurrentHashMap<String, CamelliaStatistics> map = this.map;
            this.map = new ConcurrentHashMap<>();
            Map<String, CamelliaStatsData> dataMap = new HashMap<>();
            for (Map.Entry<String, CamelliaStatistics> entry : map.entrySet()) {
                dataMap.put(entry.getKey(), entry.getValue().getStatsDataAndReset());
            }
            return dataMap;
        } catch (Exception e) {
            logger.error("getStatsDataAndReset error", e);
            return null;
        }
    }

    public Map<String, CamelliaStatsData> getStatsData() {
        try {
            Map<String, CamelliaStatsData> dataMap = new HashMap<>();
            for (Map.Entry<String, CamelliaStatistics> entry : map.entrySet()) {
                dataMap.put(entry.getKey(), entry.getValue().getStatsData());
            }
            return dataMap;
        } catch (Exception e) {
            logger.error("getStatsData error", e);
            return null;
        }
    }
}
