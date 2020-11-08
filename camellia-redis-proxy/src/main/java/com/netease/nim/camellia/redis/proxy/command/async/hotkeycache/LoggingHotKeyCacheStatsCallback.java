package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class LoggingHotKeyCacheStatsCallback implements HotKeyCacheStatsCallback {

    private static final Logger logger = LoggerFactory.getLogger("hotKeyCacheStats");

    @Override
    public void callback(HotKeyCacheStats hotKeyCacheStats) {
        try {
            if (hotKeyCacheStats == null) return;
            List<HotKeyCacheStats.Stats> list = hotKeyCacheStats.getStatsList();
            if (list == null) return;
            logger.warn("====hot-key-cache-stats====");
            for (HotKeyCacheStats.Stats stats : list) {
                logger.warn("hot-key-cache-stats, key = {}, hit-count = {}", SafeEncoder.encode(stats.getKey()), stats.getHitCount());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
