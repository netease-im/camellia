package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils;


/**
 * Created by caojiajun on 2024/5/31
 */
public class ZSetRank {
    private final int start;
    private final int stop;
    private final int size;

    public ZSetRank(int start, int stop, int size) {
        if (start < 0) {
            start += size;
        }
        if (stop < 0) {
            stop += size;
        }
        if (start < 0) {
            start = 0;
        }
        this.start = start;
        this.stop = stop;
        this.size = size;
    }

    public boolean isEmptyRank() {
        return stop < 0 || start > stop;
    }

    public int getSize() {
        return size;
    }

    public int getStart() {
        return start;
    }

    public int getStop() {
        return stop;
    }
}
