package com.netease.nim.camellia.redis.proxy.command.async.hotkeycache;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class LoggingHotKeyCacheStatsCallback implements HotKeyCacheStatsCallback {

    private static final Logger logger = LoggerFactory.getLogger("hotKeyCacheStats");

    @Override
    public void callback(CommandContext commandContext, HotKeyCacheStats hotKeyCacheStats, CommandHotKeyCacheConfig config) {
        try {
            if (hotKeyCacheStats == null) return;
            List<HotKeyCacheStats.Stats> list = hotKeyCacheStats.getStatsList();
            if (list == null) return;
            logger.warn("====hot-key-cache-stats====");
            logger.warn("command.context = {}", commandContext);
            for (HotKeyCacheStats.Stats stats : list) {
                logger.warn("hot-key-cache-stats, key = {}, hitCount = {}, checkMillis = {}, checkThreshold = {}",
                        Utils.bytesToString(stats.getKey()), stats.getHitCount(), config.getCounterCheckMillis(), config.getCounterCheckThreshold());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
