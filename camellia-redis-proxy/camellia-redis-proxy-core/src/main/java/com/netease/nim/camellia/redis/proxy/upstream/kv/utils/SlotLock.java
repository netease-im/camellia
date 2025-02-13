package com.netease.nim.camellia.redis.proxy.upstream.kv.utils;

import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2024/9/10
 */
public class SlotLock {

    private final ReentrantLock[] locks = new ReentrantLock[RedisClusterCRC16Utils.SLOT_SIZE];

    public SlotLock() {
        for (int i = 0; i< locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public ReentrantLock getLock(int slot) {
        return locks[slot];
    }
}
