package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class LoggingHotKeyCacheStatsCallback implements HotKeyCacheStatsCallback {

    private static final Logger logger = LoggerFactory.getLogger("camellia.redis.proxy.hotKeyCacheStats");

    @Override
    public void callback(IdentityInfo identityInfo, HotKeyCacheInfo hotKeyCacheStats, long checkMillis, long checkThreshold) {
        try {
            if (hotKeyCacheStats == null) return;
            List<HotKeyCacheInfo.Stats> list = hotKeyCacheStats.getStatsList();
            if (list == null) return;
            logger.warn("====hot-key-cache-stats====");
            logger.warn("identify.info = {}", identityInfo);
            for (HotKeyCacheInfo.Stats stats : list) {
                logger.warn("hot-key-cache-stats, key = {}, hitCount = {}, checkMillis = {}, checkThreshold = {}",
                        Utils.bytesToString(stats.getKey()), stats.getHitCount(), checkMillis, checkThreshold);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
