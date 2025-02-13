package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2024/6/3
 */
public class HotKeyCalculator {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCalculator.class);

    private final String namespace;
    private final KeyType keyType;
    private ConcurrentLinkedHashMap<BytesKey, Node> cache;
    private int threshold;
    private int readThreshold;
    private int writeThreshold;
    private int capacity;
    private long window;

    public HotKeyCalculator(String namespace, KeyType keyType) {
        this.namespace = namespace;
        this.keyType = keyType;
        rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, keyType + ".kv.hot.key.capacity", 100000);
        if (this.capacity != capacity) {
            if (this.cache == null) {
                this.cache = new ConcurrentLinkedHashMap.Builder<BytesKey, Node>()
                        .initialCapacity(capacity)
                        .maximumWeightedCapacity(capacity)
                        .build();
            } else {
                this.cache.setCapacity(capacity);
            }
            this.capacity = capacity;
            logger.info("kv hot key calculator cache build, namespace = {}, keyType = {}, capacity = {}", namespace, keyType, capacity);
        }

        int defaultThreshold = RedisKvConf.getInt(namespace, "kv.hot.key.threshold", 2);
        int defaultReadThreshold = RedisKvConf.getInt(namespace, "kv.hot.key.read.threshold", -1);
        int defaultWriteThreshold = RedisKvConf.getInt(namespace, "kv.hot.key.write.threshold", -1);
        int defaultWindow = RedisKvConf.getInt(namespace, "kv.hot.key.time.window", 5000);

        int threshold = RedisKvConf.getInt(namespace, keyType + ".kv.hot.key.threshold", defaultThreshold);
        int readThreshold = RedisKvConf.getInt(namespace, keyType + ".kv.hot.key.read.threshold", defaultReadThreshold);
        int writeThreshold = RedisKvConf.getInt(namespace, keyType + ".kv.hot.key.write.threshold", defaultWriteThreshold);
        int window = RedisKvConf.getInt(namespace, keyType + ".kv.hot.key.time.window", defaultWindow);

        if (this.threshold != threshold || this.readThreshold != readThreshold || this.writeThreshold != writeThreshold) {
            logger.info("kv hot key calculator cache config update, namespace = {}, keyType = {}, threshold = {}, read.threshold = {}, write.threshold = {}",
                    namespace, keyType, threshold, readThreshold, writeThreshold);
            this.threshold = threshold;
            this.readThreshold = readThreshold;
            this.writeThreshold = writeThreshold;
            if (this.threshold < 0 && this.writeThreshold < 0 && this.readThreshold < 0) {
                cache.clear();
            }
        }
        if (this.window != window) {
            logger.info("kv hot key calculator cache config update, namespace = {}, keyType = {}, window = {}", namespace, keyType, window);
            this.window = window;
        }
    }

    public boolean isHotKey(byte[] key, RedisCommand redisCommand) {
        int targetThreshold;
        if (redisCommand.getType() == RedisCommand.Type.READ) {
            targetThreshold = readThreshold < 0 ? threshold : readThreshold;
        } else {
            targetThreshold = writeThreshold < 0 ? threshold : writeThreshold;
        }
        if (targetThreshold < 0) {
            return false;
        }
        BytesKey bytesKey = new BytesKey(key);
        Node node = CamelliaMapUtils.computeIfAbsent(cache, bytesKey, key1 -> new Node());
        if (TimeCache.currentMillis - node.time > window) {
            long last = node.count.get();
            node.count.set(last / 2);
            node.time = TimeCache.currentMillis;
        }
        return node.count.incrementAndGet() > targetThreshold;
    }

    public long estimateSize() {
        long estimateSize = 0;
        for (Map.Entry<BytesKey, Node> entry : cache.entrySet()) {
            estimateSize += 24;
            estimateSize += entry.getKey().getKey().length;
        }
        return estimateSize;
    }

    public int getCapacity() {
        return capacity;
    }

    public long size() {
        return cache.size();
    }

    private static class Node {
        AtomicLong count = new AtomicLong();
        long time = System.currentTimeMillis();
    }
}
