package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
import com.netease.nim.camellia.redis.proxy.util.LRUCounter;
import com.netease.nim.camellia.redis.proxy.util.ScheduledExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2020/10/20
 */
public class HotKeyHunter {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyHunter.class);

    private final HotKeyConfig hotKeyConfig;
    private final HotKeyMonitorCallback callback;
    private final LRUCounter counter;
    private final CommandContext commandContext;

    public HotKeyHunter(CommandContext commandContext, HotKeyConfig hotKeyConfig, HotKeyMonitorCallback callback) {
        this.commandContext = commandContext;
        this.hotKeyConfig = hotKeyConfig;
        this.callback = callback;
        this.counter = new LRUCounter(hotKeyConfig.getCheckCacheMaxCapacity(),
                hotKeyConfig.getCheckCacheMaxCapacity(), hotKeyConfig.getCheckMillis());
        ScheduledExecutorUtils.scheduleAtFixedRate(this::callback, hotKeyConfig.getCheckMillis(),
                hotKeyConfig.getCheckMillis(), TimeUnit.MILLISECONDS);
        logger.info("HotKeyHunter init success, commandContext = {}", commandContext);
    }

    public void incr(byte[]... keys) {
        for (byte[] key : keys) {
            incr(key);
        }
    }

    public void incr(List<byte[]> keys) {
        for (byte[] key : keys) {
            incr(key);
        }
    }

    public void incr(byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        counter.incrementAndGet(bytesKey);
    }

    private void callback() {
        try {
            TreeSet<LRUCounter.SortedBytesKey> set = counter.getSortedCacheValue(hotKeyConfig.getCheckThreshold());
            if (set == null || set.isEmpty()) return;
            List<HotKeyInfo> hotKeys = new ArrayList<>(hotKeyConfig.getMaxHotKeyCount());
            for (LRUCounter.SortedBytesKey sortedBytesKey : set) {
                hotKeys.add(new HotKeyInfo(sortedBytesKey.getKey(), sortedBytesKey.getCount()));
                if (hotKeys.size() >= hotKeyConfig.getMaxHotKeyCount()) {
                    break;
                }
            }
            if (!hotKeys.isEmpty()) {
                callback.callback(commandContext, hotKeys, hotKeyConfig);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
