package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        int threshold = RedisKvConf.getInt(namespace, keyType + ".kv.hot.key.threshold", 10);
        int window = RedisKvConf.getInt(namespace, keyType + ".kv.hot.key.time.window", 5000);
        if (this.threshold != threshold) {
            logger.info("kv hot key calculator cache build, namespace = {}, keyType = {}, threshold = {}", namespace, keyType, threshold);
            this.threshold = threshold;
        }
        if (this.window != window) {
            logger.info("kv hot key calculator cache build, namespace = {}, keyType = {}, window = {}", namespace, keyType, window);
            this.window = window;
        }
    }

    public boolean isHotKey(byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        Node node = CamelliaMapUtils.computeIfAbsent(cache, bytesKey, key1 -> new Node());
        if (TimeCache.currentMillis - node.time > window) {
            long last = node.count.get();
            node.count.set(last / 2);
            node.time = TimeCache.currentMillis;
        }
        return node.count.incrementAndGet() > threshold;
    }

    private static class Node {
        AtomicLong count = new AtomicLong();
        long time = System.currentTimeMillis();
    }
}
