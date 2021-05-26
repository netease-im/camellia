package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
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
    public void callback(CommandContext commandContext, List<HotKeyInfo> hotKeys, HotKeyConfig hotKeyConfig) {
        try {
            logger.warn("====hot-key-stats====");
            for (HotKeyInfo hotKey : hotKeys) {
                logger.warn("hot-key, key = {}, count = {}, checkMillis = {}, checkThreshold = {}, command.context = {}",
                        Utils.bytesToString(hotKey.getKey()), hotKey.getCount(), hotKeyConfig.getCheckMillis(), hotKeyConfig.getCheckThreshold(), commandContext);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
