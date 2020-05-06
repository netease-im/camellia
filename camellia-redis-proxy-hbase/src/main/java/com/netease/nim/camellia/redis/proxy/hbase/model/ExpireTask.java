package com.netease.nim.camellia.redis.proxy.hbase.model;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 * Created by caojiajun on 2020/5/6.
 */
public class ExpireTask {

    private final byte[] key;
    private final long millisecondsTimestamp;

    public ExpireTask(byte[] key, long millisecondsTimestamp) {
        this.key = key;
        this.millisecondsTimestamp = millisecondsTimestamp;
    }

    public byte[] getKey() {
        return key;
    }

    public long getMillisecondsTimestamp() {
        return millisecondsTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpireTask that = (ExpireTask) o;
        return millisecondsTimestamp == that.millisecondsTimestamp &&
                Arrays.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(millisecondsTimestamp);
        result = 31 * result + Arrays.hashCode(key);
        return result;
    }
}
