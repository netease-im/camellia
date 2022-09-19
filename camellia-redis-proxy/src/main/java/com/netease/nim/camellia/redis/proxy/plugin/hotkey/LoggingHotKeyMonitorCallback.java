package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class LoggingHotKeyMonitorCallback implements HotKeyMonitorCallback {

    private static final Logger logger = LoggerFactory.getLogger("camellia.redis.proxy.hotKeyStats");

    @Override
    public void callback(IdentityInfo identityInfo, List<HotKeyInfo> hotKeys, long checkMillis, long checkThreshold) {
        try {
            logger.warn("====hot-key-stats====");
            for (HotKeyInfo hotKey : hotKeys) {
                logger.warn("hot-key, key = {}, count = {}, checkMillis = {}, checkThreshold = {}, identityInfo = {}",
                        Utils.bytesToString(hotKey.getKey()), hotKey.getCount(), checkMillis, checkThreshold, identityInfo);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
