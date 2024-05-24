package com.netease.nim.camellia.redis.proxy.upstream.kv.buffer;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2024/5/23
 */
public class BufferLock {

    private static final ReentrantLock[] locks = new ReentrantLock[1024];
    static {
        for (int i=0; i<locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public static ReentrantLock getLock(byte[] key) {
        int hashCode = Arrays.hashCode(key);
        if (hashCode < 0) {
            hashCode = - hashCode;
        }
        return locks[hashCode & (1024 - 1)];
    }
}
