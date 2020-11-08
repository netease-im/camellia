package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class LoggingHoyKeyMonitorCallback implements HotKeyMonitorCallback {

    private static final Logger logger = LoggerFactory.getLogger("hotKeyStats");

    @Override
    public void callback(List<HotKeyInfo> hotKeys, HotKeyConfig hotKeyConfig) {
        try {
            logger.warn("====hot-key====");
            for (HotKeyInfo hotKey : hotKeys) {
                logger.warn("hot-key, key = {}, count = {}, checkPeriodMillis = {}",
                        SafeEncoder.encode(hotKey.getKey()), hotKey.getCount(), hotKeyConfig.getCheckMillis());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
