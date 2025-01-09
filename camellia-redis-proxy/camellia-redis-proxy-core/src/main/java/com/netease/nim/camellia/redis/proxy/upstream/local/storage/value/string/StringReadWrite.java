package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.persist.ValueFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.block.StringBlockReadWrite;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/1/3
 */
public class StringReadWrite {

    private final ConcurrentHashMap<Short, SlotStringReadWrite> map = new ConcurrentHashMap<>();

    private final ValueFlushExecutor flushExecutor;
    private final StringBlockReadWrite stringBlockReadWrite;

    public StringReadWrite(ValueFlushExecutor flushExecutor, StringBlockReadWrite stringBlockReadWrite) {
        this.flushExecutor = flushExecutor;
        this.stringBlockReadWrite = stringBlockReadWrite;
    }

    public void put(short slot, KeyInfo keyInfo, byte[] data) throws IOException {
        get(slot).put(keyInfo, data);
    }

    public byte[] get(short slot, KeyInfo keyInfo) throws IOException {
        return get(slot).get(keyInfo);
    }

    public CompletableFuture<FlushResult> flush(short slot) throws IOException {
        SlotStringReadWrite slotStringReadWrite = get(slot);
        if (slotStringReadWrite == null) {
            return CompletableFuture.completedFuture(FlushResult.OK);
        }
        return slotStringReadWrite.flush();
    }

    private SlotStringReadWrite get(short slot) {
        return CamelliaMapUtils.computeIfAbsent(map, slot, s -> new SlotStringReadWrite(slot, flushExecutor, stringBlockReadWrite));
    }

    public boolean needFlush(short slot) {
        SlotStringReadWrite slotStringReadWrite = get(slot);
        if (slotStringReadWrite == null) {
            return false;
        }
        return slotStringReadWrite.needFlush();
    }

}
