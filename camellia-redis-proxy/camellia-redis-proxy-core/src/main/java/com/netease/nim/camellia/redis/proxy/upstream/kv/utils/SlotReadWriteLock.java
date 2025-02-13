package com.netease.nim.camellia.redis.proxy.upstream.kv.utils;

import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by caojiajun on 2024/9/10
 */
public class SlotReadWriteLock {

    private final ReentrantReadWriteLock[] readWriteLocks = new ReentrantReadWriteLock[RedisClusterCRC16Utils.SLOT_SIZE];

    public SlotReadWriteLock() {
        for (int i=0; i<readWriteLocks.length; i++) {
            readWriteLocks[i] = new ReentrantReadWriteLock();
        }
    }

    public ReentrantReadWriteLock getLock(int slot) {
        return readWriteLocks[slot];
    }
}
