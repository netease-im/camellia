package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.EstimateSizeValueCalculator;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.LRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.LRUCacheName;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.persist.KeyFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block.KeyBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.SlotKeyReadWrite;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/1/3
 */
public class KeyReadWrite {

    private final KeyFlushExecutor executor;
    private final KeyBlockReadWrite blockCache;

    private final ConcurrentHashMap<Short, SlotKeyReadWrite> map = new ConcurrentHashMap<>();

    private final LRUCache<Key, KeyInfo> cache;

    public KeyReadWrite(KeyFlushExecutor executor, KeyBlockReadWrite blockCache) {
        this.executor = executor;
        this.blockCache = blockCache;
        this.cache = new LRUCache<>(LRUCacheName.key_cache, 128, new EstimateSizeValueCalculator<>(), new EstimateSizeValueCalculator<>());
    }

    /**
     * get for run to completion
     * @param slot slot
     * @param key key
     * @return key info
     */
    public ValueWrapper<KeyInfo> getForRunToCompletion(short slot, Key key) {
        KeyInfo keyInfo1 = cache.get(key);
        if (keyInfo1 != null) {
            if (keyInfo1 == KeyInfo.DELETE) {
                return () -> null;
            }
            if (keyInfo1.isExpire()) {
                cache.put(key, KeyInfo.DELETE);
                return () -> null;
            }
            return () -> keyInfo1;
        }
        ValueWrapper<KeyInfo> valueWrapper = get(slot).getForRunToCompletion(key);
        if (valueWrapper != null) {
            KeyInfo keyInfo2 = valueWrapper.get();
            if (keyInfo2 != null && keyInfo2.isExpire()) {
                return () -> null;
            }
        }
        return valueWrapper;
    }

    /**
     * get key info
     * @param slot slot
     * @param key key
     * @return key info
     * @throws IOException exception
     */
    public KeyInfo get(short slot, Key key) throws IOException {
        KeyInfo keyInfo = cache.get(key);
        if (keyInfo != null) {
            if (keyInfo == KeyInfo.DELETE) {
                return null;
            }
            if (keyInfo.isExpire()) {
                cache.put(key, KeyInfo.DELETE);
                return null;
            }
            return keyInfo;
        }
        keyInfo = get(slot).get(key);
        if (keyInfo == null) {
            cache.put(key, KeyInfo.DELETE);
            return null;
        }
        if (keyInfo.isExpire()) {
            cache.put(key, KeyInfo.DELETE);
            return null;
        }
        cache.put(key, keyInfo);
        return keyInfo;
    }

    /**
     * get for compact
     * @param slot slot
     * @param key ley
     * @return key info
     * @throws IOException exception
     */
    public KeyInfo getForCompact(short slot, Key key) throws IOException {
        KeyInfo keyInfo = cache.get(key);
        if (keyInfo != null) {
            if (keyInfo == KeyInfo.DELETE) {
                return null;
            }
            return keyInfo;
        }
        keyInfo = get(slot).getForCompact(key);
        if (keyInfo == null) {
            return null;
        }
        if (keyInfo.isExpire()) {
            return null;
        }
        return keyInfo;
    }

    /**
     * put
     * @param slot slot
     * @param keyInfo key info
     */
    public void put(short slot, KeyInfo keyInfo) {
        cache.put(new Key(keyInfo.getKey()), keyInfo);
        get(slot).put(keyInfo);
    }

    /**
     * delete
     * @param slot slot
     * @param key key
     */
    public void delete(short slot, Key key) {
        cache.put(key, KeyInfo.DELETE);
        get(slot).delete(key);
    }

    /**
     * flush prepare
     * @param slot slot
     * @return key info map
     */
    public Map<Key, KeyInfo> flushPrepare(short slot) {
        SlotKeyReadWrite slotKeyReadWrite = map.get(slot);
        if (slotKeyReadWrite == null) {
            return Collections.emptyMap();
        }
        return slotKeyReadWrite.flushPrepare();
    }

    /**
     * flush key to disk
     * @param slot slot
     * @return flush result
     */
    public CompletableFuture<FlushResult> flush(short slot) {
        SlotKeyReadWrite slotKeyReadWrite = map.get(slot);
        if (slotKeyReadWrite == null) {
            return CompletableFuture.completedFuture(FlushResult.OK);
        }
        return slotKeyReadWrite.flush();
    }

    /**
     * check need flush
     * @param slot slot
     * @return result
     */
    public boolean needFlush(short slot) {
        SlotKeyReadWrite slotKeyReadWrite = map.get(slot);
        if (slotKeyReadWrite == null) {
            return false;
        }
        return slotKeyReadWrite.needFlush();
    }

    private SlotKeyReadWrite get(short slot) {
        return CamelliaMapUtils.computeIfAbsent(map, slot, s -> new SlotKeyReadWrite(slot, executor, blockCache));
    }
}
