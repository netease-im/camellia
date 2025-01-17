package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import java.util.Map;

/**
 *
 * Created by caojiajun on 2025/1/14
 */
public class WalManifest implements IWalManifest {


    @Override
    public void load() {

    }

    @Override
    public long fileId(short slot) {
        return 0;
    }

    @Override
    public long getFileWriteNextOffset(long fileId) {
        return 0;
    }

    @Override
    public void updateFileWriteNextOffset(long fileId, long nextOffset) {

    }

    @Override
    public SlotWalOffset getSlotWalOffsetEnd(short slot) {
        return null;
    }

    @Override
    public void updateSlotWalOffsetEnd(short slot, SlotWalOffset offset) {

    }

    @Override
    public void updateSlotWalOffsetStart(short slot, SlotWalOffset offset) {

    }

    @Override
    public Map<Short, SlotWalOffset> getSlotWalOffsetStartMap() {
        return Map.of();
    }
}
