package com.netease.nim.camellia.hot.key.server.monitor;

import com.netease.nim.camellia.hot.key.server.calculate.TopNCounterManager;
import com.netease.nim.camellia.hot.key.server.calculate.TopNStatsResult;

/**
 * Created by caojiajun on 2023/5/11
 */
public class TopNMonitor {

    private static TopNCounterManager manager;

    public static void register(TopNCounterManager topNCounterManager) {
        TopNMonitor.manager = topNCounterManager;
    }

    public static TopNStatsResult getTopNStatsResult(String namespace) {
        if (manager == null) {
            return null;
        }
        return manager.getTopNStats(namespace);
    }
}
