package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/10/20
 */
public class HotKeyHunter {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyHunter.class);

    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(HotKeyHunter.class));

    private final HotKeyConfig hotKeyConfig;
    private final HotKeyMonitorCallback callback;
    private final Cache<BytesKey, AtomicLong> cache;

    public HotKeyHunter(HotKeyConfig hotKeyConfig, HotKeyMonitorCallback callback) {
        this.hotKeyConfig = hotKeyConfig;
        this.callback = callback;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(hotKeyConfig.getCheckMillis()))
                .maximumSize(hotKeyConfig.getCheckCacheMaxCapacity())
                .build();
        scheduledExecutor.scheduleAtFixedRate(this::callback, hotKeyConfig.getCheckMillis(),
                        hotKeyConfig.getCheckMillis(), TimeUnit.MILLISECONDS);
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
        AtomicLong count = this.cache.get(new BytesKey(key), k -> new AtomicLong());
        if (count != null) {
            count.incrementAndGet();
        }
    }

    private void callback() {
        try {
            ConcurrentMap<BytesKey, AtomicLong> map = cache.asMap();
            TreeSet<SortedBytesKey> treeSet = new TreeSet<>();
            for (Map.Entry<BytesKey, AtomicLong> entry : map.entrySet()) {
                long count = entry.getValue().get();
                if (count >= hotKeyConfig.getCheckThreshold()) {
                    byte[] key = entry.getKey().getKey();
                    treeSet.add(new SortedBytesKey(key, count));
                }
            }
            List<HotKeyInfo> hotKeys = new ArrayList<>(hotKeyConfig.getMaxHotKeyCount());
            for (SortedBytesKey key : treeSet) {
                if (key.count < hotKeyConfig.getCheckThreshold()) break;
                hotKeys.add(new HotKeyInfo(key.key, key.count));
                if (hotKeys.size() >= hotKeyConfig.getMaxHotKeyCount()) {
                    break;
                }
            }
            if (!hotKeys.isEmpty()) {
                callback.callback(hotKeys, hotKeyConfig);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static class SortedBytesKey implements Comparable<SortedBytesKey> {
        private final byte[] key;
        private final long count;

        public SortedBytesKey(byte[] key, long count) {
            this.key = key;
            this.count = count;
        }

        @Override
        public int compareTo(SortedBytesKey o) {
            return Long.compare(o.count, count);
        }
    }
}
