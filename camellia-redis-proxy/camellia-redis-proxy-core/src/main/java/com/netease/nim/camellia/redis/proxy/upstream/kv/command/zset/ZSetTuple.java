package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.Objects;

/**
 * Created by caojiajun on 2024/5/7
 */
public class ZSetTuple {
    private final BytesKey member;
    private final BytesKey score;

    public ZSetTuple(BytesKey member, BytesKey score) {
        this.member = member;
        this.score = score;
    }

    public BytesKey getMember() {
        return member;
    }

    public BytesKey getScore() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZSetTuple zSetTuple = (ZSetTuple) o;
        return Objects.equals(member, zSetTuple.member);
    }

    @Override
    public int hashCode() {
        return Objects.hash(member);
    }
}
