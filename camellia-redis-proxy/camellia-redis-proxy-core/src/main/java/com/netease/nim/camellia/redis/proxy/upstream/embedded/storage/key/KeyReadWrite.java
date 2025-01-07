package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.persist.KeyFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.slot.KeyBlockCache;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.slot.SlotKeyReadWrite;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/1/3
 */
public class KeyReadWrite {

    private final KeyFlushExecutor executor;
    private final KeyBlockCache blockCache;

    private final ConcurrentHashMap<Short, SlotKeyReadWrite> map = new ConcurrentHashMap<>();

    public KeyReadWrite(KeyFlushExecutor executor, KeyBlockCache blockCache) {
        this.executor = executor;
        this.blockCache = blockCache;
    }

    private SlotKeyReadWrite get(short slot) {
        return CamelliaMapUtils.computeIfAbsent(map, slot, s -> new SlotKeyReadWrite(slot, executor, blockCache));
    }

    public KeyInfo get(short slot, BytesKey key) throws IOException {
        KeyInfo keyInfo = get(slot).get(key);
        if (keyInfo == null) {
            return null;
        }
        if (keyInfo.isExpire()) {
            return null;
        }
        return keyInfo;
    }

    public void put(short slot, KeyInfo keyInfo) {
        get(slot).put(keyInfo);
    }

    public void delete(short slot, BytesKey key) {
        get(slot).delete(key);
    }

    public void flushPrepare(short slot) {
        SlotKeyReadWrite slotKeyReadWrite = map.get(slot);
        if (slotKeyReadWrite == null) {
            return;
        }
        slotKeyReadWrite.flushPrepare();
    }

    public CompletableFuture<FlushResult> flush(short slot) {
        SlotKeyReadWrite slotKeyReadWrite = map.get(slot);
        if (slotKeyReadWrite == null) {
            return CompletableFuture.completedFuture(FlushResult.OK);
        }
        return slotKeyReadWrite.flush();
    }

    public boolean needFlush(short slot) {
        SlotKeyReadWrite slotKeyReadWrite = map.get(slot);
        if (slotKeyReadWrite == null) {
            return false;
        }
        return slotKeyReadWrite.needFlush();
    }
}
