package com.netease.nim.camellia.id.gen.common;

/**
 * 返回的id范围，递增的闭区间
 * Created by caojiajun on 2020/4/9.
 */
public class IDRange {
    private long start;
    private long end;

    public IDRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}
