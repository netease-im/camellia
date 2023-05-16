package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.common.netty.pack.HotKeyCacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/16
 */
public class LoggingHotKeyCacheStatsCallback implements HotKeyCacheStatsCallback {

    private static final Logger logger = LoggerFactory.getLogger("camellia-hot-key-cache-stats");

    @Override
    public void newStats(List<HotKeyCacheStats> statsList) {
        logger.info("====hot-key-cache-stats====");
        for (HotKeyCacheStats stats : statsList) {
            logger.info("namespace={},key={},hitCount={}", stats.getNamespace(), stats.getKey(), stats.getHitCount());
        }
        logger.info("====end====");
    }
}
