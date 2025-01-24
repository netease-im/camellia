package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot;

import java.util.Objects;

/**
 * Created by caojiajun on 2024/12/31
 */
public record SlotInfo(long fileId, long offset, long capacity) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlotInfo slotInfo = (SlotInfo) o;
        return fileId == slotInfo.fileId && offset == slotInfo.offset && capacity == slotInfo.capacity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, offset, capacity);
    }
}
